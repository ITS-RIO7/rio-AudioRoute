package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
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
 * 1. Manage Audio Focus and Redirection Routing for the Android system.
 * 2. Keep audio split continuously active in Lock Screen (via WakeLock & public notification).
 * 3. Auto-detect and route Bluetooth connections seamlessly in real-time.
 * 4. Run non-stop from activation until explicit deactivation.
 */
class AudioRouterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var routingManager: AudioRoutingManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val channelId = "audio_router_service_channel"
    private val notificationId = 1001

    private var isReceiverRegistered = false

    // Auto-Bluetooth connection and reconnection broadcast receiver
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    Log.d("AudioRouterService", "Bluetooth ACL Connected: ${device?.name}")
                    // Automatically re-assert audio routing
                    routingManager.refreshAudioStatus()
                    routingManager.forceCommunicationToSpeaker(true)
                    updateNotification()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d("AudioRouterService", "Bluetooth ACL Disconnected")
                    routingManager.refreshAudioStatus()
                    updateNotification()
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    routingManager.refreshAudioStatus()
                    routingManager.forceCommunicationToSpeaker(true)
                    updateNotification()
                }
            }
        }
    }

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

        // Acquire partial WakeLock to ensure audio routing and focus stay active on Lock Screen
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "rioAudioRouter:LockScreenWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.e("AudioRouterService", "Error acquiring WakeLock: ${e.message}")
        }

        // Register Bluetooth Auto-Connect event receiver
        registerBluetoothReceiver()

        // Observe hardware status to refresh notification dynamically
        serviceScope.launch {
            routingManager.hardwareStatus.collectLatest {
                if (isRunning) {
                    updateNotification()
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    private fun unregisterBluetoothReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(bluetoothReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
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
        unregisterBluetoothReceiver()
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Exception) {}
            }
        }
        wakeLock = null

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
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Manages audio focus and split audio routing with Lock Screen & Auto-BT support"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
        val routeText = if (isSpeaker) "🔊 Calls: Phone Speaker" else "🎧 Calls: Bluetooth"
        val focusText = if (status.audioFocusStatus == AudioFocusStatus.FOCUS_GAINED) "🎯 Focus: Active" else "🎯 Focus: Standby"

        val toggleLabel = if (isSpeaker) "Route BT" else "Route Speaker"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🟢 ROUTER ACTIVE (Lock Screen & Auto-BT)")
            .setContentText("🎵 Songs: $btName  |  $routeText")
            .setSubText("Auto-BT & Lock Screen ON")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_rotate, toggleLabel, pendingToggle)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deactivate", pendingStop)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification())
    }
}
