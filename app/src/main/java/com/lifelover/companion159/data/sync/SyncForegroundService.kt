package com.lifelover.companion159.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lifelover.companion159.R
import kotlinx.coroutines.*

/**
 * Foreground Service для критичної синхронізації
 * Гарантує що синхронізація завершиться навіть якщо додаток закритий
 */
class SyncForegroundService : Service() {

    companion object {
        private const val TAG = "SyncForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sync_channel"
        private const val CHANNEL_NAME = "Синхронізація даних"

        fun start(context: Context) {
            val intent = Intent(context, SyncForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            return START_NOT_STICKY
        }

        isRunning = true

        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started with notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                Log.d(TAG, "Starting sync in foreground service")

                val syncService = com.lifelover.companion159.di.ServiceLocator
                    .getSyncService(applicationContext)

                syncService.performSync()
                    .onSuccess {
                        Log.d(TAG, "✅ Sync completed successfully")
                        updateNotification("Синхронізація завершена")
                        delay(2000) // Показуємо повідомлення 2 секунди
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Sync failed: ${error.message}")
                        updateNotification("Помилка синхронізації")
                        delay(3000)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync error", e)
                updateNotification("Помилка синхронізації")
                delay(3000)
            } finally {
                isRunning = false
                Log.d(TAG, "Stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Синхронізація інвентарю з сервером"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createNotification(text: String = "Синхронізація даних..."): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Companion 159")
            .setContentText(text)
            .setSmallIcon(R.drawable.sync_check)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val notification = createNotification(text)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }
}