package com.imsidetector.domain

import com.imsidetector.data.CellTowerRecord
import timber.log.Timber
import kotlin.math.abs

/**
 * Advanced cell tower analyzer for detecting anomalies in tower behavior.
 * Implements LAC/TAC consistency checks, CID validation, and neighbor cell analysis.
 */
class CellTowerAnalyzer {
    
    private val cellHistory = mutableListOf<CellTowerRecord>()
    private val MAX_HISTORY_SIZE = 100
    
    // Known legitimate LAC/TAC ranges (simplified - in production would use telecom databases)
    private val knownLacRanges = mapOf(
        \"US\" to (1..65535),
        \"EU\" to (1..65535),
        \"ASIA\" to (1..65535)
    )
    
    /**
     * Analyze cell tower for consistency anomalies.
     * Returns Pair<anomalyScore, detectedAnomalies>
     */
    fun analyzeCellTowerConsistency(currentCell: CellTowerRecord): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // Add to history
        cellHistory.add(currentCell)
        if (cellHistory.size > MAX_HISTORY_SIZE) {
            cellHistory.removeAt(0)
        }
        
        if (cellHistory.size < 2) {
            return Pair(0, emptyList())
        }
        
        val previousCell = cellHistory[cellHistory.size - 2]
        
        // 1. Check for impossible LAC/TAC changes
        val lacTacAnomaly = checkLacTacConsistency(previousCell, currentCell)
        if (lacTacAnomaly.second.isNotEmpty()) {
            score += lacTacAnomaly.first
            anomalies.addAll(lacTacAnomaly.second)
        }
        
        // 2. Check for CID anomalies
        val cidAnomaly = checkCidConsistency(previousCell, currentCell)
        if (cidAnomaly.second.isNotEmpty()) {
            score += cidAnomaly.first
            anomalies.addAll(cidAnomaly.second)
        }
        
        // 3. Check for ARFCN/EARFCN validity
        val arfcnAnomaly = checkArfcnValidity(currentCell)
        if (arfcnAnomaly.second.isNotEmpty()) {
            score += arfcnAnomaly.first
            anomalies.addAll(arfcnAnomaly.second)
        }
        
        // 4. Check for tower hopping (rapid cell changes)
        val hoppingAnomaly = checkTowerHopping()
        if (hoppingAnomaly.second.isNotEmpty()) {
            score += hoppingAnomaly.first
            anomalies.addAll(hoppingAnomaly.second)
        }
        
        // 5. Check for neighbor cell consistency
        val neighborAnomaly = checkNeighborCells(currentCell, previousCell)
        if (neighborAnomaly.second.isNotEmpty()) {
            score += neighborAnomaly.first
            anomalies.addAll(neighborAnomaly.second)
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check LAC/TAC consistency between consecutive cells.
     */
    private fun checkLacTacConsistency(
        previous: CellTowerRecord,
        current: CellTowerRecord
    ): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // LAC/TAC change is normal when moving, but rapid changes are suspicious
        if (previous.lac != current.lac || previous.tac != current.tac) {
            // Check if this is a rapid change
            val timeDiff = current.timestamp - previous.timestamp
            
            // If LAC/TAC changed in less than 10 seconds, it's suspicious
            if (timeDiff < 10000) {
                score += 15
                anomalies.add(
                    \"SUSPICIOUS: LAC/TAC changed rapidly (${timeDiff}ms) from \" +
                    \"${previous.lac}/${previous.tac} to ${current.lac}/${current.tac}\"
                )
            }
            
            // Check for impossible LAC/TAC combinations
            if (isImpossibleLacTac(current)) {
                score += 20
                anomalies.add(\"CRITICAL: Impossible LAC/TAC combination detected (${current.lac}/${current.tac})\")
            }
        }
        
        // Check for LAC/TAC change without CID change (suspicious)
        if ((previous.lac != current.lac || previous.tac != current.tac) &&
            previous.cid == current.cid) {
            score += 10
            anomalies.add(\"ANOMALY: LAC/TAC changed but CID remained same\")
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check for CID consistency anomalies.
     */
    private fun checkCidConsistency(
        previous: CellTowerRecord,
        current: CellTowerRecord
    ): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // CID change with same LAC/TAC is suspicious
        if (current.cid != previous.cid &&
            current.lac == previous.lac &&
            current.tac == previous.tac) {
            
            score += 8
            anomalies.add(
                \"ANOMALY: Cell ID changed (${previous.cid} → ${current.cid}) \" +
                \"without LAC/TAC change\"
            )
        }
        
        // Check for CID values that are too large or invalid
        if (current.cid < 0 || current.cid > 268435455) { // Max CID for LTE
            score += 5
            anomalies.add(\"ANOMALY: Invalid CID value (${current.cid})\")
        }
        
        // Check for CID that's all zeros or all ones (suspicious)
        if (current.cid == 0L || current.cid == 268435455L) {
            score += 5
            anomalies.add(\"ANOMALY: Suspicious CID value (${current.cid})\")
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check ARFCN/EARFCN validity for the network type.
     */
    private fun checkArfcnValidity(cell: CellTowerRecord): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        when (cell.networkType) {
            \"GSM\" -> {
                // GSM ARFCN valid range: 0-1023 (P-GSM), 0-124 (DCS), 0-374 (PCS), 0-154 (GSM-850)
                if (cell.arfcn < 0 || cell.arfcn > 1023) {
                    score += 8
                    anomalies.add(\"ANOMALY: Invalid GSM ARFCN (${cell.arfcn})\")
                }
            }
            \"WCDMA\" -> {
                // WCDMA UARFCN valid range: 0-16383
                if (cell.uarfcn < 0 || cell.uarfcn > 16383) {
                    score += 8
                    anomalies.add(\"ANOMALY: Invalid WCDMA UARFCN (${cell.uarfcn})\")
                }
            }
            \"LTE\" -> {
                // LTE EARFCN valid range: 0-262143
                if (cell.earfcn < 0 || cell.earfcn > 262143) {
                    score += 8
                    anomalies.add(\"ANOMALY: Invalid LTE EARFCN (${cell.earfcn})\")
                }
            }
            \"NR\" -> {
                // 5G NR ARFCN valid range: 0-3279165
                if (cell.nrArfcn < 0 || cell.nrArfcn > 3279165) {
                    score += 8
                    anomalies.add(\"ANOMALY: Invalid NR ARFCN (${cell.nrArfcn})\")
                }
            }
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Detect tower hopping (rapid consecutive cell changes).
     */
    private fun checkTowerHopping(): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        if (cellHistory.size < 5) {
            return Pair(0, emptyList())
        }
        
        // Check last 5 cells for rapid changes
        val recentCells = cellHistory.takeLast(5)
        val timeSpan = recentCells.last().timestamp - recentCells.first().timestamp
        val uniqueCells = recentCells.map { it.cid }.distinct().size
        
        // If more than 3 unique cells in 60 seconds, it's hopping
        if (uniqueCells >= 4 && timeSpan < 60000) {
            score += 12
            anomalies.add(
                \"SUSPICIOUS: Tower hopping detected ($uniqueCells cells in ${timeSpan}ms)\"
            )
        }
        
        // Check for alternating cells (sign of fake tower)
        if (cellHistory.size >= 4) {
            val last4 = cellHistory.takeLast(4)
            if (last4[0].cid == last4[2].cid && last4[1].cid == last4[3].cid &&
                last4[0].cid != last4[1].cid) {
                score += 15
                anomalies.add(\"CRITICAL: Alternating cell pattern detected (possible fake tower)\")
            }
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check neighbor cell consistency.
     */
    private fun checkNeighborCells(
        current: CellTowerRecord,
        previous: CellTowerRecord
    ): Pair<Int, List<String>> {
        val anomalies = mutableListOf<String>()
        var score = 0
        
        // Neighbor cells disappeared
        if (previous.neighborCells.isNotEmpty() && current.neighborCells.isEmpty()) {
            score += 5
            anomalies.add(\"ANOMALY: Neighbor cells list disappeared\")
        }
        
        // Too many neighbor cells (suspicious)
        val currentNeighbors = current.neighborCells.split(\",\").filter { it.isNotEmpty() }.size
        if (currentNeighbors > 10) {
            score += 3
            anomalies.add(\"ANOMALY: Unusually high number of neighbor cells ($currentNeighbors)\")
        }
        
        // Current cell appears in neighbor list (impossible)
        if (current.neighborCells.contains(current.cid.toString())) {
            score += 10
            anomalies.add(\"CRITICAL: Current cell appears in neighbor list\")
        }
        
        return Pair(score, anomalies)
    }
    
    /**
     * Check if LAC/TAC combination is impossible.
     */
    private fun isImpossibleLacTac(cell: CellTowerRecord): Boolean {
        // LAC/TAC of 0 is typically invalid
        if (cell.lac == 0 && cell.tac == 0) return true
        
        // LAC/TAC of 65535 (0xFFFF) is typically invalid
        if (cell.lac == 65535 && cell.tac == 65535) return true
        
        // Check for patterns that indicate fake tower
        // Sequential LAC/TAC values
        if (cellHistory.size >= 3) {
            val last3 = cellHistory.takeLast(3)
            val lacs = last3.map { it.lac }
            val tacs = last3.map { it.tac }
            
            // Check for sequential pattern
            if (lacs[0] + 1 == lacs[1] && lacs[1] + 1 == lacs[2]) {
                return true
            }
            if (tacs[0] + 1 == tacs[1] && tacs[1] + 1 == tacs[2]) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Get cell history for analysis.
     */
    fun getCellHistory(): List<CellTowerRecord> {
        return cellHistory.toList()
    }
    
    /**
     * Clear history (useful for location changes).
     */
    fun clearHistory() {
        cellHistory.clear()
    }
}

