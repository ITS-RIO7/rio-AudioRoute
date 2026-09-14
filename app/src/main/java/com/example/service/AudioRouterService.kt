package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioRoutingManager

class AudioRouterService : Service() {

    private lateinit var routingManager: AudioRoutingManager
    private val channelId = "audio_router_service_channel"
    private val notificationId = 1001

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_SPEAKER = "ACTION_TOGGLE_SPEAKER"

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        routingManager = AudioRoutingManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SPEAKER -> {
                val current = routingManager.hardwareStatus.value.isCommunicationDeviceSpeaker
                routingManager.forceCommunicationToSpeaker(!current)
                updateNotification()
            }
            else -> {
                isRunning = true
                routingManager.forceCommunicationToSpeaker(true)
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(notificationId, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        routingManager.resetRoutingToDefault()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sound Router Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors audio routing to split Bluetooth music and Phone speaker calls"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRouterService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AudioRouterService::class.java).apply {
            action = ACTION_TOGGLE_SPEAKER
        }
        val pendingToggle = PendingIntent.getService(
            this,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val status = routingManager.hardwareStatus.value
        val btName = status.connectedDeviceName ?: "Bluetooth Device"
        val subtitle = "🎵 Songs: $btName  |  📞 Calls: Phone Speaker"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sound Router Active")
            .setContentText(subtitle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Toggle Speaker", pendingToggle)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification())
    }
}
