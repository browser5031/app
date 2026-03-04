package com.imsidetector.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsidetector.data.CellTowerRecord
import com.imsidetector.data.CurrentCellState
import com.imsidetector.data.DatabaseRepository
import com.imsidetector.data.ThreatEvent
import com.imsidetector.domain.CellInfoProvider
import com.imsidetector.domain.ThreatAssessmentCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for managing detector app state and UI data.
 * Coordinates between UI, database, and detection engine.
 */
class DetectorViewModel(context: Context) : ViewModel() {
    
    private val cellInfoProvider = CellInfoProvider(context)
    private val threatAssessmentCoordinator = ThreatAssessmentCoordinator(context)
    private val databaseRepository = DatabaseRepository()
    
    // UI State
    private val _currentCellState = MutableStateFlow(CurrentCellState())
    val currentCellState: StateFlow<CurrentCellState> = _currentCellState.asStateFlow()
    
    private val _threatScore = MutableStateFlow(0)
    val threatScore: StateFlow<Int> = _threatScore.asStateFlow()
    
    private val _threatLevel = MutableStateFlow(\"GREEN\")
    val threatLevel: StateFlow<String> = _threatLevel.asStateFlow()
    
    private val _recentThreats = MutableStateFlow<List<ThreatEvent>>(emptyList())
    val recentThreats: StateFlow<List<ThreatEvent>> = _recentThreats.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        initializeDatabase()
    }
    
    /**
     * Initialize database connection.
     */
    private fun initializeDatabase() {
        viewModelScope.launch {
            try {
                databaseRepository.initialize()
                Timber.d(\"Database initialized in ViewModel\")
            } catch (e: Exception) {
                Timber.e(e, \"Failed to initialize database\")
                _errorMessage.value = \"Database initialization failed\"
            }
        }
    }
    
    /**
     * Start monitoring cell information.
     */
    fun startMonitoring() {
        _isMonitoring.value = true
        updateCellInformation()
        Timber.d(\"Monitoring started\")
    }
    
    /**
     * Stop monitoring cell information.
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        Timber.d(\"Monitoring stopped\")
    }
    
    /**
     * Update cell information and perform threat assessment.
     */
    fun updateCellInformation() {
        viewModelScope.launch {
            try {
                val cellInfo = cellInfoProvider.getCurrentCellInfo() ?: return@launch
                val neighbors = cellInfoProvider.getNeighboringCells()
                
                // Perform threat assessment
                val threatAnalysis = threatAssessmentCoordinator.assessThreat(cellInfo)
                
                // Update UI state
                _currentCellState.value = CurrentCellState(
                    timestamp = cellInfo.timestamp,
                    lac = cellInfo.lac,
                    tac = cellInfo.tac,
                    cid = cellInfo.cid,
                    signalStrength = cellInfo.signalStrength,
                    signalLevel = cellInfo.signalLevel,
                    networkType = cellInfo.networkType,
                    operatorName = cellInfo.operatorName,
                    cipherStatus = cellInfo.cipherStatus,
                    cipherAlgorithm = cellInfo.cipherAlgorithm,
                    roaming = cellInfo.roaming,
                    neighborCells = neighbors.map { it.cid.toString() },
                    threatScore = threatAnalysis.overallScore,
                    threatLevel = threatAnalysis.threatLevel
                )
                
                _threatScore.value = threatAnalysis.overallScore
                _threatLevel.value = threatAnalysis.threatLevel
                
                // Store in database
                databaseRepository.insertCellTowerRecord(cellInfo)
                
                // Store threat event if threats detected
                if (threatAnalysis.detectedThreats.isNotEmpty()) {
                    val threatEvent = ThreatEvent(
                        threatType = threatAnalysis.detectedThreats.firstOrNull() ?: \"UNKNOWN\",
                        severity = threatAnalysis.overallScore,
                        threatLevel = threatAnalysis.threatLevel,
                        description = threatAnalysis.detectedThreats.joinToString(\", \"),
                        recommendedAction = threatAnalysis.recommendations.firstOrNull() ?: \"\"
                    )
                    databaseRepository.insertThreatEvent(threatEvent)
                }
                
                // Fetch recent threats for UI
                loadRecentThreats()
                
                Timber.d(
                    \"Cell info updated - Threat: ${threatAnalysis.threatLevel} \" +
                    \"(Score: ${threatAnalysis.overallScore})\"
                )
                
            } catch (e: Exception) {
                Timber.e(e, \"Error updating cell information\")
                _errorMessage.value = \"Error: ${e.message}\"
            }
        }
    }
    
    /**
     * Load recent threat events from database.
     */
    fun loadRecentThreats() {
        viewModelScope.launch {
            try {
                val threats = databaseRepository.getRecentThreatEvents(limit = 20)
                _recentThreats.value = threats
            } catch (e: Exception) {
                Timber.e(e, \"Error loading recent threats\")
            }
        }
    }
    
    /**
     * Get threat statistics.
     */
    fun getThreatStatistics(): Map<String, Any> {
        return threatAssessmentCoordinator.getThreatBreakdown(
            _currentCellState.value.let {
                CellTowerRecord(
                    timestamp = it.timestamp,
                    lac = it.lac,
                    tac = it.tac,
                    cid = it.cid,
                    signalStrength = it.signalStrength,
                    signalLevel = it.signalLevel,
                    networkType = it.networkType,
                    operatorName = it.operatorName,
                    cipherStatus = it.cipherStatus,
                    cipherAlgorithm = it.cipherAlgorithm,
                    roaming = it.roaming
                )
            }
        )
    }
    
    /**
     * Reset threat assessment (useful when changing location).
     */
    fun resetThreatAssessment() {
        threatAssessmentCoordinator.reset()
        _threatScore.value = 0
        _threatLevel.value = \"GREEN\"
        Timber.d(\"Threat assessment reset\")
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            databaseRepository.close()
        }
    }
}

