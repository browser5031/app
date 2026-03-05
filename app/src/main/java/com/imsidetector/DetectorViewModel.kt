package com.imsidetector.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

class DetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val cellInfoProvider = CellInfoProvider(application)
    private val threatAssessmentCoordinator = ThreatAssessmentCoordinator(application)
    private val databaseRepository = DatabaseRepository()

    private val _currentCellState = MutableStateFlow(CurrentCellState())
    val currentCellState: StateFlow<CurrentCellState> = _currentCellState.asStateFlow()

    private val _threatScore = MutableStateFlow(0)
    val threatScore: StateFlow<Int> = _threatScore.asStateFlow()

    private val _threatLevel = MutableStateFlow("GREEN")
    val threatLevel: StateFlow<String> = _threatLevel.asStateFlow()

    private val _recentThreats = MutableStateFlow<List<ThreatEvent>>(emptyList())
    val recentThreats: StateFlow<List<ThreatEvent>> = _recentThreats.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                databaseRepository.initialize()
                Timber.d("Database initialized in ViewModel")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize database")
            }
        }
    }

    fun startMonitoring() {
        _isMonitoring.value = true
        updateCellInformation()
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
    }

    fun updateCellInformation() {
        viewModelScope.launch {
            try {
                val cellInfo = cellInfoProvider.getCurrentCellInfo() ?: return@launch
                val neighbors = cellInfoProvider.getNeighboringCells()
                val threatAnalysis = threatAssessmentCoordinator.assessThreat(cellInfo)

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

                databaseRepository.insertCellTowerRecord(cellInfo)

                if (threatAnalysis.detectedThreats.isNotEmpty()) {
                    databaseRepository.insertThreatEvent(
                        ThreatEvent(
                            threatType = threatAnalysis.detectedThreats.firstOrNull() ?: "UNKNOWN",
                            severity = threatAnalysis.overallScore,
                            threatLevel = threatAnalysis.threatLevel,
                            description = threatAnalysis.detectedThreats.joinToString(", "),
                            recommendedAction = threatAnalysis.recommendations.firstOrNull() ?: ""
                        )
                    )
                }

                loadRecentThreats()
            } catch (e: Exception) {
                Timber.e(e, "Error updating cell information")
            }
        }
    }

    private fun loadRecentThreats() {
        viewModelScope.launch {
            try {
                _recentThreats.value = databaseRepository.getRecentThreatEvents(limit = 20)
            } catch (e: Exception) {
                Timber.e(e, "Error loading recent threats")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { databaseRepository.close() }
    }
}
