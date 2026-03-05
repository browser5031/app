package com.imsidetector.data

import java.util.UUID

data class CellTowerRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val lac: Int = -1,
    val tac: Int = -1,
    val cid: Long = -1,
    val earfcn: Int = -1,
    val pci: Int = -1,
    val rsrp: Int = -1,
    val rsrq: Int = -1,
    val cqi: Int = -1,
    val arfcn: Int = -1,
    val bsic: Int = -1,
    val rssi: Int = -1,
    val uarfcn: Int = -1,
    val psc: Int = -1,
    val rscp: Int = -1,
    val nrArfcn: Int = -1,
    val nrPci: Int = -1,
    val ssRsrp: Int = -1,
    val ssRsrq: Int = -1,
    val signalStrength: Int = -1,
    val signalLevel: Int = -1,
    val timingAdvance: Int = -1,
    val cipherStatus: String = "UNKNOWN",
    val cipherAlgorithm: String = "",
    val networkType: String = "",
    val operatorName: String = "",
    val operatorCode: String = "",
    val roaming: Boolean = false,
    val manualSelection: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val neighborCells: String = ""
)

data class ThreatEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val threatType: String = "",
    val severity: Int = 0,
    val threatLevel: String = "GREEN",
    val affectedParameter: String = "",
    val previousValue: String = "",
    val currentValue: String = "",
    val description: String = "",
    val recommendedAction: String = "",
    val cellTowerRecordId: String? = null,
    val location: String = "",
    val networkType: String = ""
)

data class BaselineProfile(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 1000f,
    val expectedLac: Int = -1,
    val expectedTac: Int = -1,
    val expectedCid: Long = -1,
    val expectedOperator: String = "",
    val expectedNetworkType: String = "",
    val minSignalStrength: Int = -120,
    val maxSignalStrength: Int = -50,
    val expectedSignalStrength: Int = -75,
    val expectedEncryption: String = "ENCRYPTED",
    val expectedCipherAlgorithm: String = "A5/3",
    val typicalNeighborCells: String = "",
    val sampleCount: Int = 0,
    val trustLevel: Int = 50
)

data class SMSLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String = "",
    val content: String = "",
    val classification: String = "NORMAL",
    val messageClass: Int = -1,
    val port: Int = -1,
    val isBlocked: Boolean = false,
    val notes: String = ""
)

data class CurrentCellState(
    val timestamp: Long = System.currentTimeMillis(),
    val lac: Int = -1,
    val tac: Int = -1,
    val cid: Long = -1,
    val signalStrength: Int = -1,
    val signalLevel: Int = -1,
    val networkType: String = "",
    val operatorName: String = "",
    val cipherStatus: String = "UNKNOWN",
    val cipherAlgorithm: String = "",
    val roaming: Boolean = false,
    val neighborCells: List<String> = emptyList(),
    val threatScore: Int = 0,
    val threatLevel: String = "GREEN"
)

data class ThreatAnalysis(
    val overallScore: Int = 0,
    val threatLevel: String = "GREEN",
    val encryptionScore: Int = 0,
    val cellConsistencyScore: Int = 0,
    val signalAnomalyScore: Int = 0,
    val protocolAnomalyScore: Int = 0,
    val detectedThreats: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
