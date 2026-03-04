package com.imsidetector.data

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mongodb.kson.ObjectId
import timber.log.Timber

/**
 * Repository for database operations using Realm.
 * Handles all persistence operations for cell records, threat events, and baselines.
 */
class DatabaseRepository {
    
    private lateinit var realm: Realm
    
    /**
     * Initialize database connection.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            realm = Realm.open()
            Timber.d(\"Database initialized successfully\")
        } catch (e: Exception) {
            Timber.e(e, \"Failed to initialize database\")
            throw e
        }
    }
    
    /**
     * Close database connection.
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            realm.close()
            Timber.d(\"Database closed\")
        } catch (e: Exception) {
            Timber.e(e, \"Error closing database\")
        }
    }
    
    // ==================== Cell Tower Records ====================
    
    /**
     * Insert or update cell tower record.
     */
    suspend fun insertCellTowerRecord(record: CellTowerRecord) = withContext(Dispatchers.IO) {
        try {
            realm.write {
                copyToRealm(record)
            }
            Timber.d(\"Cell tower record inserted: LAC=${record.lac}, TAC=${record.tac}, CID=${record.cid}\")
        } catch (e: Exception) {
            Timber.e(e, \"Error inserting cell tower record\")
        }
    }
    
    /**
     * Get all cell tower records.
     */
    suspend fun getAllCellTowerRecords(): List<CellTowerRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<CellTowerRecord>()
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving cell tower records\")
            emptyList()
        }
    }
    
    /**
     * Get recent cell tower records.
     */
    suspend fun getRecentCellTowerRecords(limit: Int = 100): List<CellTowerRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<CellTowerRecord>()
                .sort(\"timestamp\", Sort.DESCENDING)
                .limit(limit)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving recent cell tower records\")
            emptyList()
        }
    }
    
    /**
     * Get cell tower records for a specific time range.
     */
    suspend fun getCellTowerRecordsByTimeRange(
        startTime: Long,
        endTime: Long
    ): List<CellTowerRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<CellTowerRecord>(\"timestamp >= $0 AND timestamp <= $1\", startTime, endTime)
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving cell tower records by time range\")
            emptyList()
        }
    }
    
    /**
     * Get cell tower records for a specific LAC/TAC.
     */
    suspend fun getCellTowerRecordsByLacTac(lac: Int, tac: Int): List<CellTowerRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<CellTowerRecord>(\"lac == $0 AND tac == $1\", lac, tac)
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving cell tower records by LAC/TAC\")
            emptyList()
        }
    }
    
    /**
     * Delete old cell tower records (cleanup).
     */
    suspend fun deleteOldCellTowerRecords(olderThanMillis: Long) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - olderThanMillis
            realm.write {
                val oldRecords = query<CellTowerRecord>(\"timestamp < $0\", cutoffTime).find()
                delete(oldRecords)
                Timber.d(\"Deleted ${oldRecords.size} old cell tower records\")
            }
        } catch (e: Exception) {
            Timber.e(e, \"Error deleting old cell tower records\")
        }
    }
    
    // ==================== Threat Events ====================
    
    /**
     * Insert threat event.
     */
    suspend fun insertThreatEvent(event: ThreatEvent) = withContext(Dispatchers.IO) {
        try {
            realm.write {
                copyToRealm(event)
            }
            Timber.d(\"Threat event inserted: Type=${event.threatType}, Severity=${event.severity}\")
        } catch (e: Exception) {
            Timber.e(e, \"Error inserting threat event\")
        }
    }
    
    /**
     * Get all threat events.
     */
    suspend fun getAllThreatEvents(): List<ThreatEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<ThreatEvent>()
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving threat events\")
            emptyList()
        }
    }
    
    /**
     * Get recent threat events.
     */
    suspend fun getRecentThreatEvents(limit: Int = 50): List<ThreatEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<ThreatEvent>()
                .sort(\"timestamp\", Sort.DESCENDING)
                .limit(limit)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving recent threat events\")
            emptyList()
        }
    }
    
    /**
     * Get threat events by severity level.
     */
    suspend fun getThreatEventsBySeverity(minSeverity: Int): List<ThreatEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<ThreatEvent>(\"severity >= $0\", minSeverity)
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving threat events by severity\")
            emptyList()
        }
    }
    
    /**
     * Get threat events by type.
     */
    suspend fun getThreatEventsByType(threatType: String): List<ThreatEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<ThreatEvent>(\"threatType == $0\", threatType)
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving threat events by type\")
            emptyList()
        }
    }
    
    /**
     * Delete old threat events (cleanup).
     */
    suspend fun deleteOldThreatEvents(olderThanMillis: Long) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - olderThanMillis
            realm.write {
                val oldEvents = query<ThreatEvent>(\"timestamp < $0\", cutoffTime).find()
                delete(oldEvents)
                Timber.d(\"Deleted ${oldEvents.size} old threat events\")
            }
        } catch (e: Exception) {
            Timber.e(e, \"Error deleting old threat events\")
        }
    }
    
    // ==================== SMS Logs ====================
    
    /**
     * Insert SMS log.
     */
    suspend fun insertSMSLog(smsLog: SMSLog) = withContext(Dispatchers.IO) {
        try {
            realm.write {
                copyToRealm(smsLog)
            }
            Timber.d(\"SMS log inserted: From=${smsLog.sender}, Classification=${smsLog.classification}\")
        } catch (e: Exception) {
            Timber.e(e, \"Error inserting SMS log\")
        }
    }
    
    /**
     * Get suspicious SMS logs.
     */
    suspend fun getSuspiciousSMSLogs(): List<SMSLog> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<SMSLog>(\"classification != $0\", \"NORMAL\")
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving suspicious SMS logs\")
            emptyList()
        }
    }
    
    /**
     * Get silent SMS logs.
     */
    suspend fun getSilentSMSLogs(): List<SMSLog> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<SMSLog>(\"messageClass == $0\", 0)
                .sort(\"timestamp\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving silent SMS logs\")
            emptyList()
        }
    }
    
    /**
     * Delete old SMS logs (cleanup).
     */
    suspend fun deleteOldSMSLogs(olderThanMillis: Long) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - olderThanMillis
            realm.write {
                val oldLogs = query<SMSLog>(\"timestamp < $0\", cutoffTime).find()
                delete(oldLogs)
                Timber.d(\"Deleted ${oldLogs.size} old SMS logs\")
            }
        } catch (e: Exception) {
            Timber.e(e, \"Error deleting old SMS logs\")
        }
    }
    
    // ==================== Baseline Profiles ====================
    
    /**
     * Insert or update baseline profile.
     */
    suspend fun insertBaselineProfile(profile: BaselineProfile) = withContext(Dispatchers.IO) {
        try {
            realm.write {
                copyToRealm(profile)
            }
            Timber.d(\"Baseline profile inserted: Location=${profile.locationName}\")
        } catch (e: Exception) {
            Timber.e(e, \"Error inserting baseline profile\")
        }
    }
    
    /**
     * Get all baseline profiles.
     */
    suspend fun getAllBaselineProfiles(): List<BaselineProfile> = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<BaselineProfile>()
                .sort(\"updatedAt\", Sort.DESCENDING)
                .find()
                .toList()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving baseline profiles\")
            emptyList()
        }
    }
    
    /**
     * Get baseline profile by location name.
     */
    suspend fun getBaselineProfileByLocation(locationName: String): BaselineProfile? = withContext(Dispatchers.IO) {
        return@withContext try {
            realm.query<BaselineProfile>(\"locationName == $0\", locationName)
                .first()
                .find()
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving baseline profile by location\")
            null
        }
    }
    
    /**
     * Delete baseline profile.
     */
    suspend fun deleteBaselineProfile(id: ObjectId) = withContext(Dispatchers.IO) {
        try {
            realm.write {
                val profile = query<BaselineProfile>(\"id == $0\", id).first().find()
                if (profile != null) {
                    delete(profile)
                    Timber.d(\"Baseline profile deleted\")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, \"Error deleting baseline profile\")
        }
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get database statistics.
     */
    suspend fun getDatabaseStatistics(): DatabaseStatistics = withContext(Dispatchers.IO) {
        return@withContext try {
            val cellRecordCount = realm.query<CellTowerRecord>().count().find()
            val threatEventCount = realm.query<ThreatEvent>().count().find()
            val smsLogCount = realm.query<SMSLog>().count().find()
            val baselineProfileCount = realm.query<BaselineProfile>().count().find()
            
            val recentThreats = realm.query<ThreatEvent>()
                .sort(\"timestamp\", Sort.DESCENDING)
                .limit(1)
                .find()
                .toList()
            
            DatabaseStatistics(
                cellTowerRecordCount = cellRecordCount.toInt(),
                threatEventCount = threatEventCount.toInt(),
                smsLogCount = smsLogCount.toInt(),
                baselineProfileCount = baselineProfileCount.toInt(),
                lastThreatTime = recentThreats.firstOrNull()?.timestamp ?: 0
            )
        } catch (e: Exception) {
            Timber.e(e, \"Error retrieving database statistics\")
            DatabaseStatistics()
        }
    }
    
    /**
     * Clear all data (factory reset).
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            realm.write {
                deleteAll()
            }
            Timber.d(\"All data cleared\")
        } catch (e: Exception) {
            Timber.e(e, \"Error clearing all data\")
        }
    }
}

data class DatabaseStatistics(
    val cellTowerRecordCount: Int = 0,
    val threatEventCount: Int = 0,
    val smsLogCount: Int = 0,
    val baselineProfileCount: Int = 0,
    val lastThreatTime: Long = 0
)

