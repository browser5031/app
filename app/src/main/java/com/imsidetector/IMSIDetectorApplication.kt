package com.imsidetector

import android.app.Application
import android.util.Log

/**
 * Minimal Application class - no Realm, no Timber, just basics
 */
class IMSIDetectorApplication : Application() {
    
    companion object {
        private const val TAG = "IMSIDetectorApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application started - minimal version")
    }
}

