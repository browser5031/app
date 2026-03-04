package com.imsidetector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.imsidetector.MainActivity
import com.imsidetector.R
import com.imsidetector.domain.CellInfoProvider
import com.imsidetector.domain.DetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground service for continuous cell monitoring.
 * Runs in background even when app is backgrounded to provide real-time threat detection.
 */
class CellMonitoringService : Service() {
    
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var cellInfoProvider: CellInfoProvider
    private lateinit var detectionEngine: DetectionEngine
    private lateinit var notificationManager: NotificationManager
    
    private var telephonyCallback: TelephonyCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "cell_monitoring_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("CellMonitoringService created")
        
        telephonyManager = getSystemService()!!
        cellInfoProvider = CellInfoProvider(this)
        detectionEngine = DetectionEngine()
        notificationManager = getSystemService()!!
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("CellMonitoringService started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register telephony callback for cell changes
        registerTelephonyCallback()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("CellMonitoringService destroyed")
        
        unregisterTelephonyCallback()
        serviceJob.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    /**
     * Create notification channel for Android 8+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cell Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors cellular network for IMSI catcher threats"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground service notification.
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IMSI Detector Active")
            .setContentText("Monitoring cellular network...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Register telephony callback to monitor cell changes.
     */
    private fun registerTelephonyCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(),
                TelephonyCallback.CellInfoListener,
                TelephonyCallback.SignalStrengthsListener,
                TelephonyCallback.ServiceStateListener {
                
                override fun onCellInfoChanged(cellInfo: MutableList<android.telephony.CellInfo>) {
                    Timber.d("Cell info changed: ${cellInfo.size} cells")
                    serviceScope.launch {
                        processCellInfoChange()
                    }
                }
                
                override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength) {
                    Timber.d("Signal strength changed")
                    serviceScope.launch {
                        processCellInfoChange()
                    }
                }
                
                override fun onServiceStateChanged(serviceState: android.telephony.ServiceState) {
                    Timber.d("Service state changed: ${serviceState.state}")
                    serviceScope.launch {
                        processCellInfoChange()
                    }
                }
            }
            
            try {
                @Suppress("MissingPermission")
                telephonyManager.registerTelephonyCallback(
                    Dispatchers.Default.asExecutor(),
                    telephonyCallback!!
                )
                Timber.d("Telephony callback registered")
            } catch (e: Exception) {
                Timber.e(e, "Failed to register telephony callback")
            }
        }
    }
    
    /**
     * Unregister telephony callback.
     */
    private fun unregisterTelephonyCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
            try {
                @Suppress("MissingPermission")
                telephonyManager.unregisterTelephonyCallback(telephonyCallback!!)
                Timber.d("Telephony callback unregistered")
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister telephony callback")
            }
        }
    }
    
    /**
     * Process cell information change and run threat detection.
     */
    private suspend fun processCellInfoChange() {
        try {
            val currentCell = cellInfoProvider.getCurrentCellInfo() ?: return
            val neighbors = cellInfoProvider.getNeighboringCells()
            
            // Run threat analysis
            val threatAnalysis = detectionEngine.analyzeThreat(currentCell, neighbors)
            
            Timber.d(
                "Threat Analysis: Score=${threatAnalysis.overallScore}, Level=${threatAnalysis.threatLevel}, " +
                "Threats=${threatAnalysis.detectedThreats.size}"
            )
            
            // Update notification if threat detected
            if (threatAnalysis.threatLevel in listOf("ORANGE", "RED")) {
                updateThreatNotification(threatAnalysis.threatLevel, threatAnalysis.overallScore)
            }
            
            // TODO: Store in database
            // TODO: Broadcast threat event to UI
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing cell info change")
        }
    }
    
    /**
     * Update notification with threat information.
     */
    private fun updateThreatNotification(threatLevel: String, threatScore: Int) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ IMSI Catcher Threat Detected")
            .setContentText("Threat Level: $threatLevel (Score: $threatScore/100)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

