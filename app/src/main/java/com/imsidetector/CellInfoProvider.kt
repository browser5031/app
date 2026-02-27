package com.imsidetector.domain

import android.content.Context
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import com.imsidetector.data.CellTowerRecord
import timber.log.Timber

/**
 * Provides access to cellular information from Android TelephonyManager.
 * Abstracts the complexity of different cell types (GSM, WCDMA, LTE, NR).
 */
class CellInfoProvider(private val context: Context) {
    
    private val telephonyManager = context.getSystemService<TelephonyManager>()
    
    /**
     * Get current cell information as CellTowerRecord.
     */
    fun getCurrentCellInfo(): CellTowerRecord? {
        return try {
            val cellInfoList = telephonyManager?.allCellInfo ?: return null
            
            // Find the registered cell (first registered cell in the list)
            val registeredCell = cellInfoList.firstOrNull { it.isRegistered }
            
            if (registeredCell != null) {
                parseCellInfo(registeredCell)
            } else if (cellInfoList.isNotEmpty()) {
                // Fallback to first cell if no registered cell found
                parseCellInfo(cellInfoList[0])
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting cell info")
            null
        }
    }
    
    /**
     * Get all neighboring cells.
     */
    fun getNeighboringCells(): List<CellTowerRecord> {
        return try {
            val cellInfoList = telephonyManager?.allCellInfo ?: return emptyList()
            
            cellInfoList.filter { !it.isRegistered }
                .mapNotNull { parseCellInfo(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting neighboring cells")
            emptyList()
        }
    }
    
    /**
     * Get service state information.
     */
    fun getServiceState(): ServiceState? {
        return try {
            @Suppress("MissingPermission")
            telephonyManager?.serviceState
        } catch (e: Exception) {
            Timber.e(e, "Error getting service state")
            null
        }
    }
    
    /**
     * Get operator information.
     */
    fun getOperatorInfo(): Pair<String, String> {
        return try {
            val operatorName = telephonyManager?.networkOperatorName ?: "Unknown"
            val operatorCode = telephonyManager?.networkOperator ?: "Unknown"
            Pair(operatorName, operatorCode)
        } catch (e: Exception) {
            Timber.e(e, "Error getting operator info")
            Pair("Unknown", "Unknown")
        }
    }
    
    /**
     * Get network type.
     */
    fun getNetworkType(): String {
        return try {
            @Suppress("MissingPermission")
            when (telephonyManager?.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_UMTS -> "WCDMA"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                TelephonyManager.NETWORK_TYPE_NR -> "NR"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting network type")
            "UNKNOWN"
        }
    }
    
    /**
     * Check if device is roaming.
     */
    fun isRoaming(): Boolean {
        return try {
            telephonyManager?.isNetworkRoaming ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error checking roaming status")
            false
        }
    }
    
    /**
     * Parse CellInfo object into CellTowerRecord.
     */
    private fun parseCellInfo(cellInfo: CellInfo): CellTowerRecord? {
        return when (cellInfo) {
            is CellInfoLte -> parseLteCell(cellInfo)
            is CellInfoGsm -> parseGsmCell(cellInfo)
            is CellInfoWcdma -> parseWcdmaCell(cellInfo)
            is CellInfoNr -> parseNrCell(cellInfo)
            else -> null
        }
    }
    
    /**
     * Parse LTE cell information.
     */
    private fun parseLteCell(cellInfo: CellInfoLte): CellTowerRecord {
        val identity = cellInfo.cellIdentity as CellIdentityLte
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthLte
        
        val (operatorName, operatorCode) = getOperatorInfo()
        
        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            lac = identity.tac,
            tac = identity.tac,
            cid = identity.ci,
            earfcn = identity.earfcn,
            pci = identity.pci,
            rsrp = signal.dbm,
            rsrq = signal.rsrq,
            cqi = signal.cqi,
            signalStrength = signal.dbm,
            signalLevel = signal.level,
            timingAdvance = signal.timingAdvance,
            networkType = "LTE",
            operatorName = operatorName,
            operatorCode = operatorCode,
            roaming = isRoaming(),
            cipherStatus = "ENCRYPTED", // Will be updated by encryption monitor
            cipherAlgorithm = "A5/3"
        )
    }
    
    /**
     * Parse GSM cell information.
     */
    private fun parseGsmCell(cellInfo: CellInfoGsm): CellTowerRecord {
        val identity = cellInfo.cellIdentity as CellIdentityGsm
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthGsm
        
        val (operatorName, operatorCode) = getOperatorInfo()
        
        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            lac = identity.lac,
            cid = identity.cid.toLong(),
            arfcn = identity.arfcn,
            bsic = identity.bsic,
            rssi = signal.dbm,
            signalStrength = signal.dbm,
            signalLevel = signal.level,
            timingAdvance = signal.timingAdvance,
            networkType = "GSM",
            operatorName = operatorName,
            operatorCode = operatorCode,
            roaming = isRoaming(),
            cipherStatus = "ENCRYPTED",
            cipherAlgorithm = "A5/3"
        )
    }
    
    /**
     * Parse WCDMA (3G) cell information.
     */
    private fun parseWcdmaCell(cellInfo: CellInfoWcdma): CellTowerRecord {
        val identity = cellInfo.cellIdentity as CellIdentityWcdma
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthWcdma
        
        val (operatorName, operatorCode) = getOperatorInfo()
        
        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            lac = identity.lac,
            cid = identity.cid.toLong(),
            uarfcn = identity.uarfcn,
            psc = identity.psc,
            rscp = signal.dbm,
            signalStrength = signal.dbm,
            signalLevel = signal.level,
            networkType = "WCDMA",
            operatorName = operatorName,
            operatorCode = operatorCode,
            roaming = isRoaming(),
            cipherStatus = "ENCRYPTED",
            cipherAlgorithm = "A5/3"
        )
    }
    
    /**
     * Parse 5G NR cell information.
     */
    private fun parseNrCell(cellInfo: CellInfoNr): CellTowerRecord {
        val identity = cellInfo.cellIdentity as CellIdentityNr
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
        
        val (operatorName, operatorCode) = getOperatorInfo()
        
        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            nrArfcn = identity.nrArfcn,
            nrPci = identity.pci,
            ssRsrp = signal.dbm,
            ssRsrq = signal.rsrq,
            signalStrength = signal.dbm,
            signalLevel = signal.level,
            networkType = "NR",
            operatorName = operatorName,
            operatorCode = operatorCode,
            roaming = isRoaming(),
            cipherStatus = "ENCRYPTED",
            cipherAlgorithm = "A5/3"
        )
    }
}
