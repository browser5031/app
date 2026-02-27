package com.imsidetector.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kson.ObjectId
import java.util.Date

/**
 * Represents a recorded cell tower with all relevant information.
 * Used for historical tracking and baseline analysis.
 */
data class CellTowerRecord(
    @PrimaryKey
    val id: ObjectId = ObjectId(),
    val timestamp: Long = System.currentTimeMillis(),
    
    // GSM/WCDMA/LTE common
    val lac: Int = -1,  // Location Area Code
    val tac: Int = -1,  // Tracking Area Code (LTE)
    val cid: Long = -1, // Cell ID
    
    // LTE specific
    val earfcn: Int = -1,        // E-UTRA Absolute Radio Frequency Channel Number
    val pci: Int = -1,           // Physical Cell ID
    val rsrp: Int = -1,          // Reference Signal Received Power (dBm)
    val rsrq: Int = -1,          // Reference Signal Received Quality (dB)
    val cqi: Int = -1,           // Channel Quality Indicator
    
    // GSM specific
    val arfcn: Int = -1,         // Absolute Radio Frequency Channel Number
    val bsic: Int = -1,          // Base Station Identity Code
    val rssi: Int = -1,          // Received Signal Strength Indicator (dBm)
    
    // WCDMA specific
    val uarfcn: Int = -1,        // UTRA Absolute Radio Frequency Channel Number
    val psc: Int = -1,           // Primary Scrambling Code
    val rscp: Int = -1,          // Received Signal Code Power (dBm)
    
    // 5G NR specific
    val nrArfcn: Int = -1,       // NR Absolute Radio Frequency Channel Number
    val nrPci: Int = -1,         // NR Physical Cell ID
    val ssRsrp: Int = -1,        // SS Reference Signal Received Power (dBm)
    val ssRsrq: Int = -1,        // SS Reference Signal Received Quality (dB)
    
    // Common signal metrics
    val signalStrength: Int = -1, // dBm
    val signalLevel: Int = -1,    // 0-4 bars
    val timingAdvance: Int = -1,  // Timing advance value
    
    // Encryption
    val cipherStatus: String = "UNKNOWN", // "ENCRYPTED", "UNENCRYPTED", "DOWNGRADED"
    val cipherAlgorithm: String = "",     // "A5/0", "A5/1", "A5/3", etc.
    
    // Network info
    val networkType: String = "",  // "GSM", "WCDMA", "LTE", "NR"
    val operatorName: String = "",
    val operatorCode: String = "",
    val roaming: Boolean = false,
    val manualSelection: Boolean = false,
    
    // Location
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    
    // Neighbor cells (comma-separated)
    val neighborCells: String = ""
) : RealmObject

/**
 * Represents a detected threat event with severity and details.
 */
data class ThreatEvent(
    @PrimaryKey
    val id: ObjectId = ObjectId(),
    val timestamp: Long = System.currentTimeMillis(),
    
    // Threat classification
    val threatType: String = "", // "ENCRYPTION_DOWNGRADE", "LAC_TAC_CHANGE", "SIGNAL_ANOMALY", etc.
    val severity: Int = 0,       // 0-100 threat score
    val threatLevel: String = "GREEN", // "GREEN", "YELLOW", "ORANGE", "RED"
    
    // Affected parameters
    val affectedParameter: String = "",
    val previousValue: String = "",
    val currentValue: String = "",
    
    // Details
    val description: String = "",
    val recommendedAction: String = "",
    
    // Context
    val cellTowerRecordId: ObjectId? = null,
    val location: String = "",
    val networkType: String = ""
) : RealmObject

/**
 * Baseline profile for normal network behavior at a location or time.
 */
data class BaselineProfile(
    @PrimaryKey
    val id: ObjectId = ObjectId(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Location-based baseline
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 1000f,
    
    // Expected values
    val expectedLac: Int = -1,
    val expectedTac: Int = -1,
    val expectedCid: Long = -1,
    val expectedOperator: String = "",
    val expectedNetworkType: String = "",
    
    // Signal ranges
    val minSignalStrength: Int = -120,
    val maxSignalStrength: Int = -50,
    val expectedSignalStrength: Int = -75,
    
    // Encryption expectation
    val expectedEncryption: String = "ENCRYPTED",
    val expectedCipherAlgorithm: String = "A5/3",
    
    // Typical neighbor cells (comma-separated)
    val typicalNeighborCells: String = "",
    
    // Statistics
    val sampleCount: Int = 0,
    val trustLevel: Int = 50 // 0-100, higher = more trusted
) : RealmObject

/**
 * SMS log for monitoring silent SMS and suspicious messages.
 */
data class SMSLog(
    @PrimaryKey
    val id: ObjectId = ObjectId(),
    val timestamp: Long = System.currentTimeMillis(),
    
    val sender: String = "",
    val content: String = "",
    val classification: String = "NORMAL", // "NORMAL", "SILENT_SMS", "WAP_PUSH", "SUSPICIOUS"
    val messageClass: Int = -1, // 0 = Flash/Silent SMS
    val port: Int = -1,         // Destination port for binary SMS
    
    val isBlocked: Boolean = false,
    val notes: String = ""
) : RealmObject

/**
 * Current cell information state (not persisted, used for UI state).
 */
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

/**
 * Threat analysis result with detailed breakdown.
 */
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
