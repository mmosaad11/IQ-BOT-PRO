package com.iqbotpro.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.iqbotpro.app.data.network.PythonBotEngine

class BotForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.iqbotpro.app.START_BOT"
        const val ACTION_STOP = "com.iqbotpro.app.STOP_BOT"

        const val EXTRA_EMAIL = "email"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_ACCOUNT_MODE = "account_mode"
        const val EXTRA_STRATEGY_MODE = "strategy_mode"
        const val EXTRA_REAL_CONFIRMED = "real_confirmed"

        private const val CHANNEL_ID = "iqbot_foreground"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var engine: PythonBotEngine

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }

        val python = Python.getInstance()
        val osModule = python.getModule("os")
        val environ = requireNotNull(osModule.get("environ")) {
            "Python os.environ is unavailable"
        }
        environ.callAttr("__setitem__", "IQBOT_DATA_DIR", filesDir.absolutePath)

        engine = PythonBotEngine()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {
                if (::engine.isInitialized) {
                    engine.stopBot()
                }

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val mode = intent.getStringExtra(EXTRA_ACCOUNT_MODE)
                    ?.uppercase() ?: "PRACTICE"

                val email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
                val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()

                val strategyMode =
                    intent.getStringExtra(EXTRA_STRATEGY_MODE) ?: "both"

                val realConfirmed =
                    intent.getBooleanExtra(EXTRA_REAL_CONFIRMED, false)

                if (mode !in listOf("PRACTICE", "REAL")) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (mode == "REAL" && !realConfirmed) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (email.isBlank() || password.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                val notification = buildNotification()

                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                try {
                    val config = hashMapOf<String, Any>(
                        "email" to email,
                        "password" to password,
                        "account_mode" to mode,
"real_confirmed" to realConfirmed,                        
"strategy_mode" to strategyMode
                    )

                    engine.startBot(config)

                } catch (e: Exception) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IQ BOT PRO",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description = "حالة تشغيل البوت"

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("IQ BOT PRO")
            .setContentText("البوت يعمل في الخلفية")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (::engine.isInitialized) {
            engine.stopBot()
        }

        super.onDestroy()
    }
}
