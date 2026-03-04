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
     * Returns Pair of (score, threats) where score is 0-100 (higher is worse).
     */
    fun analyzeEncryption(cellInfo: CellTowerInfo): Pair<Int, List<String>> {
        var score = 0
        val threats = mutableListOf<String>()
        
        try {
            // Check for A5/0 (no encryption) - critical vulnerability
            if (cellInfo.networkType == "GSM" && cellInfo.signalStrength < -95) {
                score += 50
                threats.add("ENCRYPTION: Weak GSM signal - potential A5/0 (no encryption)")
                Timber.w("Weak GSM signal detected - potential A5/0 usage")
            }
            
            // Check for 2G network (weaker encryption)
            if (cellInfo.networkType == "GSM" || cellInfo.networkType == "EDGE") {
                score += 20
                threats.add("ENCRYPTION: 2G network with limited encryption")
                Timber.d("2G network detected - limited encryption")
            }
            
            // Check for 3G (better encryption than 2G)
            if (cellInfo.networkType == "UMTS" || cellInfo.networkType == "HSPA") {
                score += 10
                threats.add("ENCRYPTION: 3G network - moderate encryption")
                Timber.d("3G network - moderate encryption")
            }
            
            // LTE/5G have strong encryption - no score penalty
            if (cellInfo.networkType == "LTE" || cellInfo.networkType == "NR") {
                Timber.d("LTE/5G network - strong encryption")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing encryption")
            score = 30 // Return moderate score on error
            threats.add("ENCRYPTION: Error analyzing encryption status")
        }
        
        return Pair(score.coerceIn(0, 100), threats)
    }
    
    /**
     * Check for potential encryption downgrade attacks.
     * Returns Pair of (score, threats).
     */
    fun detectNetworkDowngrade(
        previousNetworkType: String,
        currentNetworkType: String
    ): Pair<Int, List<String>> {
        var score = 0
        val threats = mutableListOf<String>()
        
        // Detect suspicious network downgrades (4G/5G -> 2G)
        val suspiciousDowngrade = when {
            (previousNetworkType == "LTE" || previousNetworkType == "NR") &&
            (currentNetworkType == "GSM" || currentNetworkType == "EDGE") -> {
                score = 30
                threats.add("DOWNGRADE: Suspicious downgrade from $previousNetworkType to $currentNetworkType")
                true
            }
            
            previousNetworkType == "UMTS" && currentNetworkType == "GSM" -> {
                score = 20
                threats.add("DOWNGRADE: Network downgraded from 3G to 2G")
                true
            }
            
            else -> false
        }
        
        if (suspiciousDowngrade) {
            Timber.w("Potential downgrade attack detected: $previousNetworkType -> $currentNetworkType")
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Check for manual network selection (can be sign of attack).
     * Returns Pair of (score, threats).
     */
    fun checkManualNetworkSelection(isManualSelection: Boolean): Pair<Int, List<String>> {
        var score = 0
        val threats = mutableListOf<String>()
        
        if (isManualSelection) {
            score = 15
            threats.add("NETWORK: Manual network selection enabled (potential security risk)")
            Timber.w("Manual network selection detected")
        }
        
        return Pair(score, threats)
    }
    
    /**
     * Get cipher recommendation based on algorithm.
     */
    fun getCipherRecommendation(cipherAlgorithm: String?): String {
        return when (cipherAlgorithm) {
            "A5/0", null -> "⚠️ CRITICAL: Use A5/3 or A5/4 encryption"
            "A5/1" -> "⚠️ WARNING: Upgrade to A5/3 or A5/4 encryption"
            "A5/2" -> "⚠️ WARNING: A5/2 is weak, use A5/3 or A5/4"
            "A5/3" -> "✓ GOOD: A5/3 encryption is adequate"
            "A5/4" -> "✓ EXCELLENT: A5/4 provides strong encryption"
            else -> "⚠️ Unknown cipher algorithm: $cipherAlgorithm"
        }
    }
    
    /**
     * Get cipher strength rating (0-100, higher is better).
     */
    fun getCipherStrengthRating(cipherAlgorithm: String?): Int {
        return when (cipherAlgorithm) {
            "A5/4" -> 100
            "A5/3" -> 80
            "A5/1" -> 40
            "A5/2" -> 20
            "A5/0", null -> 0
            else -> 50 // Unknown cipher
        }
    }
    
    /**
     * Check if cipher is acceptable for secure communications.
     */
    fun isCipherAcceptable(cipherAlgorithm: String?): Boolean {
        return when (cipherAlgorithm) {
            "A5/3", "A5/4" -> true
            else -> false
        }
    }
}
