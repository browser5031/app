package com.imsidetector

import android.app.Application
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import com.imsidetector.data.CellTowerRecord
import com.imsidetector.data.ThreatEvent
import com.imsidetector.data.BaselineProfile
import com.imsidetector.data.SMSLog
import timber.log.Timber

/**
 * Application class for global initialization.
 */
class IMSIDetectorApplication : Application() {
    
    companion object {
        lateinit var realm: Realm
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize Realm database
        initializeRealm()
        
        Timber.d("IMSI Detector Application initialized")
    }
    
    private fun initializeRealm() {
        try {
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    CellTowerRecord::class,
                    ThreatEvent::class,
                    BaselineProfile::class,
                    SMSLog::class
                )
            )
            .name("imsi-detector.realm")
            .schemaVersion(1)
            .build()
            
            realm = Realm.open(config)
            Timber.d("Realm database initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Realm database")
            // Don't crash the app, just log the error
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        if (::realm.isInitialized) {
            realm.close()
        }
    }
}
