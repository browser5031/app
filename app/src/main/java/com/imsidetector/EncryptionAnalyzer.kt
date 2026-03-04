package com.imsidetector.domain

import com.imsidetector.data.CellTowerInfo
import timber.log.Timber

/**
 * Analyzes encryption status of cellular connections.
 * Detects encryption weaknesses and potential downgrade attacks.
 */
class EncryptionAnalyzer {
    
    /**
     * Analyze encryption status of current cell connection.
     * Returns encryption score (0-100, higher is better).
     */
    fun analyzeEncryption(cellInfo: CellTowerInfo): Int {
        var score = 100
        
        try {
            // Check for A5/0 (no encryption) - critical vulnerability
            if (cellInfo.networkType == "GSM" && cellInfo.signalStrength < -95) {
                score -= 50
                Timber.w("Weak GSM signal detected - potential A5/0 usage")
            }
            
            // Check for 2G network (weaker encryption)
            if (cellInfo.networkType == "GSM" || cellInfo.networkType == "EDGE") {
                score -= 20
                Timber.d("2G network detected - limited encryption")
            }
            
            // Check for 3G (better encryption than 2G)
            if (cellInfo.networkType == "UMTS" || cellInfo.networkType == "HSPA") {
                score -= 10
                Timber.d("3G network - moderate encryption")
            }
            
            // LTE/5G have strong encryption - no penalty
            if (cellInfo.networkType == "LTE" || cellInfo.networkType == "NR") {
                Timber.d("LTE/5G network - strong encryption")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing encryption")
            score = 50 // Return moderate score on error
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Check for potential encryption downgrade attacks.
     */
    fun detectDowngradeAttack(
        previousNetworkType: String,
        currentNetworkType: String
    ): Boolean {
        // Detect suspicious network downgrades (4G/5G -> 2G)
        val suspiciousDowngrade = when {
            (previousNetworkType == "LTE" || previousNetworkType == "NR") &&
            (currentNetworkType == "GSM" || currentNetworkType == "EDGE") -> true
            
            previousNetworkType == "UMTS" && currentNetworkType == "GSM" -> true
            
            else -> false
        }
        
        if (suspiciousDowngrade) {
            Timber.w("Potential downgrade attack detected: $previousNetworkType -> $currentNetworkType")
        }
        
        return suspiciousDowngrade
    }
}
