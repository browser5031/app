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

class CellInfoProvider(private val context: Context) {

    private val telephonyManager = context.getSystemService<TelephonyManager>()

    fun getCurrentCellInfo(): CellTowerRecord? {
        return try {
            @Suppress("MissingPermission")
            val cellInfoList = telephonyManager?.allCellInfo ?: return null
            val registeredCell = cellInfoList.firstOrNull { it.isRegistered }

            if (registeredCell != null) {
                parseCellInfo(registeredCell)
            } else if (cellInfoList.isNotEmpty()) {
                parseCellInfo(cellInfoList[0])
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting cell info")
            null
        }
    }

    fun getNeighboringCells(): List<CellTowerRecord> {
        return try {
            @Suppress("MissingPermission")
            val cellInfoList = telephonyManager?.allCellInfo ?: return emptyList()
            cellInfoList.filter { !it.isRegistered }.mapNotNull { parseCellInfo(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting neighboring cells")
            emptyList()
        }
    }

    fun getOperatorInfo(): Pair<String, String> {
        return try {
            val operatorName = telephonyManager?.networkOperatorName ?: "Unknown"
            val operatorCode = telephonyManager?.networkOperator ?: "Unknown"
            Pair(operatorName, operatorCode)
        } catch (e: Exception) {
            Pair("Unknown", "Unknown")
        }
    }

    fun isRoaming(): Boolean {
        return try {
            telephonyManager?.isNetworkRoaming ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun parseCellInfo(cellInfo: CellInfo): CellTowerRecord? {
        return when (cellInfo) {
            is CellInfoLte -> parseLteCell(cellInfo)
            is CellInfoGsm -> parseGsmCell(cellInfo)
            is CellInfoWcdma -> parseWcdmaCell(cellInfo)
            is CellInfoNr -> parseNrCell(cellInfo)
            else -> null
        }
    }

    private fun parseLteCell(cellInfo: CellInfoLte): CellTowerRecord {
        val identity = cellInfo.cellIdentity
        val signal = cellInfo.cellSignalStrength
        val (operatorName, operatorCode) = getOperatorInfo()

        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            lac = identity.tac,
            tac = identity.tac,
            cid = identity.ci.toLong(),
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
            cipherStatus = "ENCRYPTED",
            cipherAlgorithm = "A5/3"
        )
    }

    private fun parseGsmCell(cellInfo: CellInfoGsm): CellTowerRecord {
        val identity = cellInfo.cellIdentity
        val signal = cellInfo.cellSignalStrength
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

    private fun parseWcdmaCell(cellInfo: CellInfoWcdma): CellTowerRecord {
        val identity = cellInfo.cellIdentity
        val signal = cellInfo.cellSignalStrength
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

    private fun parseNrCell(cellInfo: CellInfoNr): CellTowerRecord {
        val identity = cellInfo.cellIdentity as CellIdentityNr
        val signal = cellInfo.cellSignalStrength as CellSignalStrengthNr
        val (operatorName, operatorCode) = getOperatorInfo()

        return CellTowerRecord(
            timestamp = System.currentTimeMillis(),
            nrArfcn = identity.nrarfcn,
            nrPci = identity.pci,
            ssRsrp = signal.dbm,
            ssRsrq = signal.ssRsrq,
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
