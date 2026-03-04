package com.imsidetector.domain

import com.imsidetector.data.CellTowerRecord
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Advanced signal strength analyzer for detecting anomalies.
 * Implements statistical analysis, trend detection, and pattern recognition.
 */
class SignalAnalyzer {
    
    private val signalHistory = mutableListOf<SignalSnapshot>()
    private val MAX_HISTORY_SIZE = 50
    
    data class SignalSnapshot(
        val timestamp: Long,
        val rssi: Int,
        val rsrp: Int,
        val rsrq: Int,
        val level: Int
    )
    
    /**
     * Analyze signal strength for anomalies.
     * Returns Pair<anomalyScore, detectedAnomalies>
     */
    fun analyzeSignalAnomalies(cell: CellTowerRecord): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // Record signal snapshot
        val snapshot = SignalSnapshot(
            timestamp = cell.timestamp,
            rssi = cell.rssi,
            rsrp = cell.rsrp,
            rsrq = cell.rsrq,
            level = cell.signalLevel
        )
        
        signalHistory.add(snapshot)
        if (signalHistory.size > MAX_HISTORY_SIZE) {
            signalHistory.removeAt(0)
        }
        
        if (signalHistory.size < 2) {
            return Pair(0, emptyList())
        }
        
        // 1. Check for sudden signal changes
        val suddenChangeAnomaly = checkSuddenSignalChange()
        if (suddenChangeAnomaly.second.isNotEmpty()) {
            score += suddenChangeAnomaly.first
            anomalies.addAll(suddenChangeAnomaly.second)
        }
        
        // 2. Check for oscillating signal (sign of fake tower)
        val oscillationAnomaly = checkSignalOscillation()
        if (oscillationAnomaly.second.isNotEmpty()) {
            score += oscillationAnomaly.first
            anomalies.addAll(oscillationAnomaly.second)
        }
        
        // 3. Check for unusually strong signal
        val strongSignalAnomaly = checkUnusuallyStrongSignal()
        if (strongSignalAnomaly.second.isNotEmpty()) {
            score += strongSignalAnomaly.first
            anomalies.addAll(strongSignalAnomaly.second)
        }
        
        // 4. Check for signal quality issues
        val qualityAnomaly = checkSignalQuality(cell)
        if (qualityAnomaly.second.isNotEmpty()) {
            score += qualityAnomaly.first
            anomalies.addAll(qualityAnomaly.second)
        }
        
        // 5. Check for timing advance anomalies
        val timingAnomaly = checkTimingAdvance(cell)
        if (timingAnomaly.second.isNotEmpty()) {
            score += timingAnomaly.first
            anomalies.addAll(timingAnomaly.second)
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Detect sudden signal strength changes.
     */
    private fun checkSuddenSignalChange(): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        if (signalHistory.size < 2) {
            return Pair(0, emptyList())
        }
        
        val current = signalHistory.last()
        val previous = signalHistory[signalHistory.size - 2]
        
        val signalChange = abs(current.rsrp - previous.rsrp)
        
        // Change of more than 15 dBm in one step is suspicious
        if (signalChange > 15) {
            score += 8
            anomalies.add(
                \"ANOMALY: Sudden signal change (${signalChange}dBm) from \" +
                \"${previous.rsrp}dBm to ${current.rsrp}dBm\"
            )
        }
        
        // Change of more than 25 dBm is very suspicious
        if (signalChange > 25) {
            score += 12
            anomalies.add(
                \"SUSPICIOUS: Extreme signal change (${signalChange}dBm) - possible fake tower\"
            )
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Detect oscillating signal pattern (sign of fake tower).
     */
    private fun checkSignalOscillation(): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        if (signalHistory.size < 5) {
            return Pair(0, emptyList())
        }
        
        // Check last 5 samples for oscillation
        val recentSignals = signalHistory.takeLast(5).map { it.rsrp }
        
        // Count direction changes (oscillations)
        var oscillations = 0
        for (i in 1 until recentSignals.size - 1) {
            val prev = recentSignals[i - 1]
            val curr = recentSignals[i]
            val next = recentSignals[i + 1]
            
            // Check if current value is local max or min
            if ((curr > prev && curr > next) || (curr < prev && curr < next)) {
                oscillations++
            }
        }
        
        // 3+ oscillations in 5 samples is suspicious
        if (oscillations >= 3) {
            score += 10
            anomalies.add(
                \"SUSPICIOUS: Signal oscillation pattern detected ($oscillations changes in 5 samples)\"
            )
        }
        
        // Check for regular oscillation pattern
        if (signalHistory.size >= 10) {
            val last10 = signalHistory.takeLast(10).map { it.rsrp }
            val pattern = last10.zipWithNext { a, b -> if (a < b) 1 else -1 }
            
            // Check if pattern repeats
            val patternStr = pattern.joinToString(\"\")
            if (patternStr.contains(\"1-1\") || patternStr.contains(\"-11\")) {
                score += 8
                anomalies.add(\"ANOMALY: Regular oscillation pattern detected\")
            }
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check for unusually strong signal.
     */
    private fun checkUnusuallyStrongSignal(): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        val current = signalHistory.last()
        
        // RSRP better than -50 dBm is very unusual (typical is -75 to -120)
        if (current.rsrp > -50) {
            score += 5
            anomalies.add(\"ANOMALY: Unusually strong signal (${current.rsrp}dBm)\")
        }
        
        // RSRQ better than -5 dB is suspicious
        if (current.rsrq > -5) {
            score += 3
            anomalies.add(\"ANOMALY: Unusually high signal quality (${current.rsrq}dB)\")
        }
        
        // Check if signal is consistently too strong
        if (signalHistory.size >= 5) {
            val recentSignals = signalHistory.takeLast(5).map { it.rsrp }
            val avgSignal = recentSignals.average()
            
            if (avgSignal > -50) {
                score += 7
                anomalies.add(
                    \"SUSPICIOUS: Consistently strong signal (avg: ${avgSignal.toInt()}dBm) \" +
                    \"may indicate fake tower\"
                )
            }
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check for signal quality issues.
     */
    private fun checkSignalQuality(cell: CellTowerRecord): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // RSRQ worse than -20 dB indicates poor quality
        if (cell.rsrq < -20) {
            score += 3
            anomalies.add(\"ANOMALY: Poor signal quality (RSRQ: ${cell.rsrq}dB)\")
        }
        
        // Check for CQI (Channel Quality Indicator) issues
        if (cell.cqi in 0..4) {
            score += 2
            anomalies.add(\"ANOMALY: Very poor channel quality (CQI: ${cell.cqi})\")
        }
        
        // Mismatch between RSRP and RSRQ (suspicious)
        if (cell.rsrp > -100 && cell.rsrq < -15) {
            score += 5
            anomalies.add(
                \"ANOMALY: RSRP/RSRQ mismatch - strong signal but poor quality \" +
                \"(RSRP: ${cell.rsrp}, RSRQ: ${cell.rsrq})\"
            )
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check for timing advance anomalies.
     */
    private fun checkTimingAdvance(cell: CellTowerRecord): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // Timing advance > 63 indicates very far distance (suspicious in urban areas)
        if (cell.timingAdvance > 63) {
            score += 4
            anomalies.add(
                \"ANOMALY: Large timing advance (${cell.timingAdvance}) indicates very distant tower\"
            )
        }
        
        // Check for timing advance changes
        if (signalHistory.size >= 2) {
            val current = signalHistory.last()
            val previous = signalHistory[signalHistory.size - 2]
            
            // Timing advance shouldn't change much unless moving
            val taChange = abs(cell.timingAdvance - (signalHistory[signalHistory.size - 2].rsrp))
            if (taChange > 20) {
                score += 3
                anomalies.add(\"ANOMALY: Rapid timing advance change detected\")
            }
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Get signal statistics for the last N samples.
     */
    fun getSignalStatistics(samples: Int = 10): SignalStatistics {
        if (signalHistory.isEmpty()) {
            return SignalStatistics()
        }
        
        val recentSignals = signalHistory.takeLast(samples).map { it.rsrp }
        
        val avg = recentSignals.average()
        val min = recentSignals.minOrNull() ?: 0
        val max = recentSignals.maxOrNull() ?: 0
        val variance = recentSignals.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)
        
        return SignalStatistics(
            average = avg.toInt(),
            min = min,
            max = max,
            stdDev = stdDev.toInt(),
            sampleCount = recentSignals.size
        )
    }
    
    /**
     * Clear history.
     */
    fun clearHistory() {
        signalHistory.clear()
    }
}

data class SignalStatistics(
    val average: Int = 0,
    val min: Int = 0,
    val max: Int = 0,
    val stdDev: Int = 0,
    val sampleCount: Int = 0
)

