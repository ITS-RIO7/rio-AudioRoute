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
import com.example.data.model.AudioFocusStatus
import com.example.data.model.RouteTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service component to:
 * 1. Manage Audio Focus for the Android system.
 * 2. Handle audio redirection routing (e.g. splitting Bluetooth music & Phone speaker calls).
 * 3. Keep audio focus and routing alive in the background.
 */
class AudioRouterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var routingManager: AudioRoutingManager
    private val channelId = "audio_router_service_channel"
    private val notificationId = 1001

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_SPEAKER = "ACTION_TOGGLE_SPEAKER"
        const val ACTION_FORCE_SPEAKER = "ACTION_FORCE_SPEAKER"
        const val ACTION_FORCE_BLUETOOTH = "ACTION_FORCE_BLUETOOTH"
        const val ACTION_REQUEST_FOCUS = "ACTION_REQUEST_FOCUS"
        const val ACTION_ABANDON_FOCUS = "ACTION_ABANDON_FOCUS"

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        routingManager = AudioRoutingManager(applicationContext)
        createNotificationChannel()

        // Observe hardware status to refresh notification dynamically
        serviceScope.launch {
            routingManager.hardwareStatus.collectLatest {
                if (isRunning) {
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SPEAKER -> {
                val isCurrentlySpeaker = routingManager.hardwareStatus.value.isSpeakerphoneForced ||
                    routingManager.hardwareStatus.value.isCommunicationDeviceSpeaker
                if (isCurrentlySpeaker) {
                    routingManager.forceCommunicationToBluetooth()
                } else {
                    routingManager.forceCommunicationToSpeaker(true)
                }
                updateNotification()
            }
            ACTION_FORCE_SPEAKER -> {
                routingManager.forceCommunicationToSpeaker(true)
                updateNotification()
            }
            ACTION_FORCE_BLUETOOTH -> {
                routingManager.forceCommunicationToBluetooth()
                updateNotification()
            }
            ACTION_REQUEST_FOCUS -> {
                routingManager.requestAudioFocus(forCommunication = true)
                updateNotification()
            }
            ACTION_ABANDON_FOCUS -> {
                routingManager.abandonAudioFocus()
                updateNotification()
            }
            else -> {
                isRunning = true
                // Request audio focus to manage system audio streams
                routingManager.requestAudioFocus(forCommunication = true)
                // Enforce initial redirection to speaker for communication/calls
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
        routingManager.abandonAudioFocus()
        routingManager.resetRoutingToDefault()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sound Router & Audio Focus Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manages audio focus and routes audio between Bluetooth and Phone Speaker"
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
        val btName = status.connectedDeviceName ?: "No Bluetooth Connected"
        val isSpeaker = status.isSpeakerphoneForced || status.isCommunicationDeviceSpeaker
        val routeText = if (isSpeaker) "🔊 Calls -> Phone Speaker" else "🎧 Calls -> Bluetooth"
        val focusText = if (status.audioFocusStatus == AudioFocusStatus.FOCUS_GAINED) "🎯 Focus: Active" else "🎯 Focus: Standby"

        val toggleLabel = if (isSpeaker) "Switch to BT" else "Switch Speaker"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Audio Router & Focus Active")
            .setContentText("$routeText  |  $focusText")
            .setSubText(btName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_rotate, toggleLabel, pendingToggle)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification())
    }
}
