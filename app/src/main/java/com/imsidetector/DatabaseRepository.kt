package com.imsidetector.data

import timber.log.Timber

class DatabaseRepository {

    private val cellTowerRecords = mutableListOf<CellTowerRecord>()
    private val threatEvents = mutableListOf<ThreatEvent>()
    private val smsLogs = mutableListOf<SMSLog>()
    private val baselineProfiles = mutableListOf<BaselineProfile>()

    suspend fun initialize() {
        Timber.d("In-memory database initialized")
    }

    suspend fun close() {
        Timber.d("Database closed")
    }

    suspend fun insertCellTowerRecord(record: CellTowerRecord) {
        cellTowerRecords.add(0, record)
        if (cellTowerRecords.size > 500) cellTowerRecords.removeAt(cellTowerRecords.lastIndex)
        Timber.d("Cell tower record inserted: LAC=${record.lac}, CID=${record.cid}")
    }

    suspend fun getRecentCellTowerRecords(limit: Int = 100): List<CellTowerRecord> {
        return cellTowerRecords.take(limit)
    }

    suspend fun insertThreatEvent(event: ThreatEvent) {
        threatEvents.add(0, event)
        if (threatEvents.size > 200) threatEvents.removeAt(threatEvents.lastIndex)
        Timber.d("Threat event inserted: Type=${event.threatType}, Severity=${event.severity}")
    }

    suspend fun getRecentThreatEvents(limit: Int = 50): List<ThreatEvent> {
        return threatEvents.take(limit)
    }

    suspend fun insertSMSLog(smsLog: SMSLog) {
        smsLogs.add(0, smsLog)
        Timber.d("SMS log inserted: From=${smsLog.sender}")
    }

    suspend fun clearAllData() {
        cellTowerRecords.clear()
        threatEvents.clear()
        smsLogs.clear()
        baselineProfiles.clear()
        Timber.d("All data cleared")
    }
}

data class DatabaseStatistics(
    val cellTowerRecordCount: Int = 0,
    val threatEventCount: Int = 0,
    val smsLogCount: Int = 0,
    val baselineProfileCount: Int = 0,
    val lastThreatTime: Long = 0
)
