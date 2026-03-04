package com.imsidetector.domain

import com.imsidetector.data.CellTowerRecord
import com.imsidetector.data.ThreatAnalysis
import timber.log.Timber

/**
 * Core threat detection engine that analyzes cell information and generates threat scores.
 * Implements multiple detection techniques in parallel and combines results into unified threat score.
 */
class DetectionEngine {
    
    private var previousCellState: CellTowerRecord? = null
    private var baselineSignalStrength: Int = -75 // dBm
    private var signalStrengthHistory: MutableList<Int> = mutableListOf()
    private val SIGNAL_HISTORY_SIZE = 20
    
    /**
     * Analyze current cell state and generate threat assessment.
     */
    fun analyzeThreat(
        currentCell: CellTowerRecord,
        previousCells: List<CellTowerRecord> = emptyList()
    ): ThreatAnalysis {
        var encryptionScore = 0
        var cellConsistencyScore = 0
        var signalAnomalyScore = 0
        var protocolAnomalyScore = 0
        val detectedThreats = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // 1. Encryption Analysis (highest severity)
        val encryptionAnalysis = analyzeEncryption(currentCell)
        encryptionScore = encryptionAnalysis.first
        if (encryptionAnalysis.second.isNotEmpty()) {
            detectedThreats.addAll(encryptionAnalysis.second)
            recommendations.add("Ensure your device uses strong encryption (A5/3 or better)")
        }
        
        // 2. Cell Consistency Analysis
        if (previousCellState != null) {
            val cellAnalysis = analyzeCellConsistency(currentCell, previousCellState!!, previousCells)
            cellConsistencyScore = cellAnalysis.first
            if (cellAnalysis.second.isNotEmpty()) {
                detectedThreats.addAll(cellAnalysis.second)
                recommendations.add("Monitor cell tower changes - verify tower legitimacy")
            }
        }
        
        // 3. Signal Anomaly Analysis
        val signalAnalysis = analyzeSignalAnomalies(currentCell)
        signalAnomalyScore = signalAnalysis.first
        if (signalAnalysis.second.isNotEmpty()) {
            detectedThreats.addAll(signalAnalysis.second)
            recommendations.add("Check signal strength trends - sudden changes may indicate fake tower")
        }
        
        // 4. Protocol Anomaly Analysis
        val protocolAnalysis = analyzeProtocolAnomalies(currentCell)
        protocolAnomalyScore = protocolAnalysis.first
        if (protocolAnalysis.second.isNotEmpty()) {
            detectedThreats.addAll(protocolAnalysis.second)
            recommendations.add("Review SMS and call logs for suspicious activity")
        }
        
        // Calculate overall threat score with weighted contributions
        val overallScore = calculateWeightedScore(
            encryptionScore = encryptionScore,
            cellConsistencyScore = cellConsistencyScore,
            signalAnomalyScore = signalAnomalyScore,
            protocolAnomalyScore = protocolAnomalyScore
        )
        
        val threatLevel = getThreatLevel(overallScore)
        
        // Update state for next analysis
        previousCellState = currentCell
        
        Timber.d("Threat Analysis - Overall: $overallScore, Level: $threatLevel, Threats: ${detectedThreats.size}")
        
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
     * Analyze encryption status - highest severity detection.
     * Returns Pair<score, threats>
     */
    private fun analyzeEncryption(cell: CellTowerRecord): Pair<Int, List<String>> {
        val threats = mutableListOf<String>()
        var score = 0
        
        when (cell.cipherStatus) {
            "UNENCRYPTED" -> {
                score = 40 // Maximum points for no encryption
                threats.add("CRITICAL: No encryption detected (A5/0 cipher)")
            }
            "DOWNGRADED" -> {
                score = 35
                threats.add("WARNING: Cipher downgrade detected (${cell.cipherAlgorithm})")
            }
            "ENCRYPTED" -> {
                // Check if cipher is weak
                if (cell.cipherAlgorithm == "A5/1" || cell.cipherAlgorithm == "A5/2") {
                    score = 20
                    threats.add("WEAK: Weak cipher algorithm detected (${cell.cipherAlgorithm})")
                }
            }
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Analyze cell tower consistency and behavior.
     * Returns Pair<score, threats>
     */
    private fun analyzeCellConsistency(
        current: CellTowerRecord,
        previous: CellTowerRecord,
        history: List<CellTowerRecord>
    ): Pair<Int, List<String>> {
        val threats = mutableListOf<String>()
        var score = 0
        
        // Check LAC/TAC changes
        if (current.lac != previous.lac || current.tac != previous.tac) {
            // LAC/TAC change is normal when moving between cells, but rapid changes are suspicious
            if (history.size > 2) {
                val recentChanges = history.takeLast(3).count { 
                    it.lac != current.lac || it.tac != current.tac 
                }
                if (recentChanges >= 2) {
                    score += 15
                    threats.add("SUSPICIOUS: Rapid LAC/TAC changes detected")
                }
            }
        }
        
        // Check CID consistency
        if (current.cid != previous.cid) {
            // CID change with same LAC/TAC is suspicious
            if (current.lac == previous.lac && current.tac == previous.tac) {
                score += 10
                threats.add("ANOMALY: Cell ID changed without LAC/TAC change")
            }
        }
        
        // Check for impossible signal patterns
        if (current.signalStrength > -50) {
            // Very strong signal in urban area might indicate fake tower
            if (history.isNotEmpty() && history.last().signalStrength < -90) {
                score += 10
                threats.add("ANOMALY: Sudden extreme signal strength increase")
            }
        }
        
        // Check neighbor cell consistency
        if (current.neighborCells.isEmpty() && previous.neighborCells.isNotEmpty()) {
            score += 5
            threats.add("ANOMALY: Neighbor cells disappeared")
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Analyze signal strength anomalies.
     * Returns Pair<score, threats>
     */
    private fun analyzeSignalAnomalies(cell: CellTowerRecord): Pair<Int, List<String>> {
        val threats = mutableListOf<String>()
        var score = 0
        
        // Track signal history
        signalStrengthHistory.add(cell.signalStrength)
        if (signalStrengthHistory.size > SIGNAL_HISTORY_SIZE) {
            signalStrengthHistory.removeAt(0)
        }
        
        if (signalStrengthHistory.size >= 5) {
            // Calculate signal trend
            val recentSignals = signalStrengthHistory.takeLast(5)
            val avgSignal = recentSignals.average()
            val maxChange = recentSignals.zipWithNext { a, b -> kotlin.math.abs(a - b) }.maxOrNull() ?: 0
            
            // Detect sudden changes (>15 dBm in one step)
            if (maxChange > 15) {
                score += 8
                threats.add("ANOMALY: Sudden signal strength change (${maxChange}dBm)")
            }
            
            // Detect oscillating signal (sign of fake tower)
            val oscillations = recentSignals.zipWithNext { a, b -> if ((a - b) * (b - (recentSignals.getOrNull(recentSignals.indexOf(b) + 1) ?: b)) < 0) 1 else 0 }.sum()
            if (oscillations >= 3) {
                score += 7
                threats.add("ANOMALY: Oscillating signal pattern detected")
            }
        }
        
        // Check for unusually good signal quality
        if (cell.rsrq != -1 && cell.rsrq > -5) {
            score += 5
            threats.add("ANOMALY: Unusually high signal quality (${cell.rsrq}dB)")
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Analyze protocol-level anomalies (SMS, calls, etc.).
     * Returns Pair<score, threats>
     */
    private fun analyzeProtocolAnomalies(cell: CellTowerRecord): Pair<Int, List<String>> {
        val threats = mutableListOf<String>()
        var score = 0
        
        // Check for LTE/3G downgrade
        if (previousCellState != null && previousCellState!!.networkType == "LTE") {
            if (cell.networkType in listOf("WCDMA", "GSM")) {
                score += 15
                threats.add("CRITICAL: Forced downgrade from LTE to ${cell.networkType}")
            }
        }
        
        // Check for manual network selection (suspicious if enabled)
        if (cell.manualSelection) {
            score += 5
            threats.add("WARNING: Manual network selection is enabled")
        }
        
        // Check for roaming in unexpected location
        if (cell.roaming && previousCellState != null && !previousCellState!!.roaming) {
            score += 3
            threats.add("INFO: Device switched to roaming")
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Calculate weighted threat score from individual components.
     */
    private fun calculateWeightedScore(
        encryptionScore: Int,
        cellConsistencyScore: Int,
        signalAnomalyScore: Int,
        protocolAnomalyScore: Int
    ): Int {
        // Weights: encryption is most critical
        val weights = mapOf(
            "encryption" to 0.40,
            "cellConsistency" to 0.30,
            "signalAnomaly" to 0.20,
            "protocolAnomaly" to 0.10
        )
        
        val weighted = (encryptionScore * weights["encryption"]!! +
                cellConsistencyScore * weights["cellConsistency"]!! +
                signalAnomalyScore * weights["signalAnomaly"]!! +
                protocolAnomalyScore * weights["protocolAnomaly"]!!).toInt()
        
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
}
