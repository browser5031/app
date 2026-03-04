package com.imsidetector.domain

import android.content.Context
import com.imsidetector.data.CellTowerRecord
import com.imsidetector.data.ThreatAnalysis
import timber.log.Timber

/**
 * Coordinates all threat detection modules and produces unified threat assessment.
 * Combines results from encryption, signal, cell tower, and protocol analyzers.
 */
class ThreatAssessmentCoordinator(context: Context) {
    
    private val encryptionAnalyzer = EncryptionAnalyzer()
    private val signalAnalyzer = SignalAnalyzer()
    private val cellTowerAnalyzer = CellTowerAnalyzer()
    private val detectionEngine = DetectionEngine()
    
    private var previousNetworkType: String = ""
    
    /**
     * Perform comprehensive threat assessment.
     */
    fun assessThreat(cell: CellTowerRecord): ThreatAnalysis {
        val detectedThreats = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        var encryptionScore = 0
        var cellConsistencyScore = 0
        var signalAnomalyScore = 0
        var protocolAnomalyScore = 0
        
        // 1. Encryption Analysis (highest priority)
        val encryptionResult = encryptionAnalyzer.analyzeEncryption(cell)
        encryptionScore = encryptionResult.first
        detectedThreats.addAll(encryptionResult.second)
        
        if (encryptionScore > 0) {
            recommendations.add(encryptionAnalyzer.getCipherRecommendation(cell.cipherAlgorithm))
        }
        
        // 2. Network Downgrade Detection
        if (previousNetworkType.isNotEmpty()) {
            val downgradeResult = encryptionAnalyzer.detectNetworkDowngrade(
                previousNetworkType,
                cell.networkType
            )
            protocolAnomalyScore += downgradeResult.first
            detectedThreats.addAll(downgradeResult.second)
        }
        
        // 3. Manual Network Selection Check
        val manualSelectionResult = encryptionAnalyzer.checkManualNetworkSelection(cell.manualSelection)
        protocolAnomalyScore += manualSelectionResult.first
        detectedThreats.addAll(manualSelectionResult.second)
        
        // 4. Cell Tower Consistency Analysis
        val cellTowerResult = cellTowerAnalyzer.analyzeCellTowerConsistency(cell)
        cellConsistencyScore = cellTowerResult.first
        detectedThreats.addAll(cellTowerResult.second)
        
        if (cellConsistencyScore > 0) {
            recommendations.add("Monitor cell tower changes - verify tower legitimacy")
        }
        
        // 5. Signal Strength Analysis
        val signalResult = signalAnalyzer.analyzeSignalAnomalies(cell)
        signalAnomalyScore = signalResult.first
        detectedThreats.addAll(signalResult.second)
        
        if (signalAnomalyScore > 0) {
            recommendations.add("Check signal strength trends - sudden changes may indicate fake tower")
        }
        
        // Calculate overall threat score with weighted contributions
        val overallScore = calculateWeightedScore(
            encryptionScore = encryptionScore,
            cellConsistencyScore = cellConsistencyScore,
            signalAnomalyScore = signalAnomalyScore,
            protocolAnomalyScore = protocolAnomalyScore
        )
        
        val threatLevel = getThreatLevel(overallScore)
        
        // Add general recommendations
        if (recommendations.isEmpty()) {
            when (threatLevel) {
                "GREEN" -> recommendations.add("Network appears secure - no threats detected")
                "YELLOW" -> recommendations.add("Minor anomalies detected - continue monitoring")
                "ORANGE" -> recommendations.add("Significant threats detected - verify network legitimacy")
                "RED" -> recommendations.add("Critical threat detected - move to safe location immediately")
            }
        }
        
        // Update state
        previousNetworkType = cell.networkType
        
        Timber.d(
            "Threat Assessment - Overall: $overallScore, Level: $threatLevel, " +
            "Encryption: $encryptionScore, CellConsistency: $cellConsistencyScore, " +
            "Signal: $signalAnomalyScore, Protocol: $protocolAnomalyScore, " +
            "Threats: ${detectedThreats.size}"
        )
        
        return ThreatAnalysis(
            overallScore = overallScore,
            threatLevel = threatLevel,
            encryptionScore = encryptionScore,
            cellConsistencyScore = cellConsistencyScore,
            signalAnomalyScore = signalAnomalyScore,
            protocolAnomalyScore = protocolAnomalyScore,
            detectedThreats = detectedThreats,
            recommendations = recommendations
        )
    }
    
    /**
     * Calculate weighted threat score from individual components.
     * Weights are optimized for IMSI catcher detection.
     */
    private fun calculateWeightedScore(
        encryptionScore: Int,
        cellConsistencyScore: Int,
        signalAnomalyScore: Int,
        protocolAnomalyScore: Int
    ): Int {
        // Weights: encryption is most critical indicator of IMSI catcher
        val weights = mapOf(
            "encryption" to 0.45,        // Highest weight - A5/0 is definitive
            "cellConsistency" to 0.30,   // Cell tower behavior is strong indicator
            "signalAnomaly" to 0.15,     // Signal patterns are secondary
            "protocolAnomaly" to 0.10    // Protocol issues are lowest priority
        )
        
        val weighted = (
            encryptionScore * weights["encryption"]!! +
            cellConsistencyScore * weights["cellConsistency"]!! +
            signalAnomalyScore * weights["signalAnomaly"]!! +
            protocolAnomalyScore * weights["protocolAnomaly"]!!
        ).toInt()
        
        return kotlin.math.min(weighted, 100) // Cap at 100
    }
    
    /**
     * Convert threat score to threat level.
     */
    private fun getThreatLevel(score: Int): String {
        return when {
            score <= 20 -> "GREEN"
            score <= 50 -> "YELLOW"
            score <= 75 -> "ORANGE"
            else -> "RED"
        }
    }
    
    /**
     * Get detailed threat breakdown.
     */
    fun getThreatBreakdown(cell: CellTowerRecord): Map<String, Any> {
        return mapOf(
            "encryption" to mapOf(
                "cipher" to cell.cipherAlgorithm,
                "status" to cell.cipherStatus,
                "strength" to encryptionAnalyzer.getCipherStrengthRating(cell.cipherAlgorithm),
                "acceptable" to encryptionAnalyzer.isCipherAcceptable(cell.cipherAlgorithm)
            ),
            "signal" to mapOf(
                "rsrp" to cell.rsrp,
                "rsrq" to cell.rsrq,
                "level" to cell.signalLevel,
                "statistics" to signalAnalyzer.getSignalStatistics()
            ),
            "cellTower" to mapOf(
                "lac" to cell.lac,
                "tac" to cell.tac,
                "cid" to cell.cid,
                "networkType" to cell.networkType,
                "operator" to cell.operatorName
            ),
            "location" to mapOf(
                "latitude" to cell.latitude,
                "longitude" to cell.longitude,
                "accuracy" to cell.accuracy
            )
        )
    }
    
    /**
     * Reset analyzers (useful when changing location).
     */
    fun reset() {
        signalAnalyzer.clearHistory()
        cellTowerAnalyzer.clearHistory()
        previousNetworkType = ""
        Timber.d("Threat assessment coordinator reset")
    }
}

