package com.imsidetector

import android.app.Application
import timber.log.Timber

class IMSIDetectorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("IMSI Detector Application started")
    }
}
