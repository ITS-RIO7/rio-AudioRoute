package com.example.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.AppAudioRule
import com.example.data.model.AppCategory
import com.example.data.model.AudioHardwareStatus
import com.example.data.model.AudioStreamConfig
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.RouteTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRoutingManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _hardwareStatus = MutableStateFlow(AudioHardwareStatus())
    val hardwareStatus: StateFlow<AudioHardwareStatus> = _hardwareStatus.asStateFlow()

    private var a2dpProfile: BluetoothProfile? = null
    private var headsetProfile: BluetoothProfile? = null

    private var isCallStateListening = false

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = proxy
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProfile = proxy
            }
            refreshAudioStatus()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpProfile = null
            } else if (profile == BluetoothProfile.HEADSET) {
                headsetProfile = null
            }
            refreshAudioStatus()
        }
    }

    init {
        initBluetoothProfiles()
        setupCallStateListener()
        refreshAudioStatus()
    }

    private fun initBluetoothProfiles() {
        try {
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error initializing Bluetooth profiles: ${e.message}")
        }
    }

    fun refreshAudioStatus() {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasBtA2dp = outputDevices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }

        var connectedBtName: String? = null
        val btDevice = outputDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        if (btDevice != null) {
            connectedBtName = btDevice.productName?.toString()
        }

        var isSpeakerForCalls = false
        var commDeviceName = "Default"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val commDev = audioManager.communicationDevice
            if (commDev != null) {
                commDeviceName = commDev.productName.toString()
                isSpeakerForCalls = commDev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        } else {
            @Suppress("DEPRECATION")
            isSpeakerForCalls = audioManager.isSpeakerphoneOn
            commDeviceName = if (isSpeakerForCalls) "Phone Speaker" else "Standard Device"
        }

        val modeStr = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL -> "IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "COMMUNICATION"
            AudioManager.MODE_RINGTONE -> "RINGTONE"
            else -> "NORMAL"
        }

        _hardwareStatus.value = _hardwareStatus.value.copy(
            isBluetoothA2dpConnected = hasBtA2dp,
            connectedDeviceName = connectedBtName,
            isCommunicationDeviceSpeaker = isSpeakerForCalls,
            communicationDeviceName = commDeviceName,
            audioMode = modeStr
        )
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<BluetoothDeviceInfo> {
        val result = mutableListOf<BluetoothDeviceInfo>()
        try {
            val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
            val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val btConnectedNames = outputDevices.filter {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }.map { it.productName.toString() }

            for (dev in bonded) {
                val name = dev.name ?: "Unknown Bluetooth Device"
                val isConn = btConnectedNames.any { it.equals(name, ignoreCase = true) }
                val typeName = when (dev.bluetoothClass?.majorDeviceClass) {
                    android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "Headphones / Audio"
                    android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> "Computer Audio"
                    android.bluetooth.BluetoothClass.Device.Major.PHONE -> "Phone"
                    else -> "Bluetooth Audio"
                }
                result.add(
                    BluetoothDeviceInfo(
                        address = dev.address,
                        name = name,
                        isConnected = isConn,
                        isBonded = true,
                        deviceType = typeName
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Cannot list bonded devices: ${e.message}")
        }
        return result
    }

    fun applyRoutingConfig(config: AudioStreamConfig) {
        if (!config.isDualRoutingActive) {
            resetRoutingToDefault()
            return
        }

        // Apply call routing specifically to Phone Speaker if selected
        if (config.callTarget == RouteTarget.SPEAKER) {
            forceCommunicationToSpeaker(true)
        } else if (config.callTarget == RouteTarget.BLUETOOTH) {
            forceCommunicationToBluetooth()
        } else {
            resetRoutingToDefault()
        }

        refreshAudioStatus()
    }

    fun forceCommunicationToSpeaker(force: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (force) {
                    val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        audioManager.setCommunicationDevice(speaker)
                    }
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.mode = if (force) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = force
            }
            refreshAudioStatus()
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Failed to force speaker: ${e.message}")
        }
    }

    fun forceCommunicationToBluetooth() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val btDev = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (btDev != null) {
                    audioManager.setCommunicationDevice(btDev)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
            }
            refreshAudioStatus()
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Failed to force bluetooth: ${e.message}")
        }
    }

    fun resetRoutingToDefault() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                @Suppress("DEPRECATION")
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            refreshAudioStatus()
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Failed to reset communication device: ${e.message}")
        }
    }

    private fun setupCallStateListener() {
        if (isCallStateListening || telephonyManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val executor = ContextCompat.getMainExecutor(context)
                telephonyManager.registerTelephonyCallback(
                    executor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallStateChange(state)
                        }
                    }
                )
                isCallStateListening = true
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChange(state)
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE)
                isCallStateListening = true
            }
        } catch (e: SecurityException) {
            Log.w("AudioRoutingManager", "Call state permission not yet granted")
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error registering call listener: ${e.message}")
        }
    }

    private fun handleCallStateChange(state: Int) {
        val stateName = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            TelephonyManager.CALL_STATE_OFFHOOK -> "CALL_IN_PROGRESS"
            else -> "IDLE"
        }
        _hardwareStatus.value = _hardwareStatus.value.copy(currentCallState = stateName)

        if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
            // Auto enforce speaker for call if desired
            forceCommunicationToSpeaker(true)
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            refreshAudioStatus()
        }
    }

    fun getInstalledAppsWithRules(): List<AppAudioRule> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppAudioRule>()

        for (info in installedApps) {
            // Skip pure background system daemons without launch or UI
            val appLabel = pm.getApplicationLabel(info).toString()
            val packageName = info.packageName
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val category = classifyApp(packageName, appLabel, info)
            val defaultRoute = when (category) {
                AppCategory.MUSIC -> RouteTarget.BLUETOOTH
                AppCategory.COMMUNICATION -> RouteTarget.SPEAKER
                AppCategory.VIDEO -> RouteTarget.DEFAULT
                AppCategory.GAME -> RouteTarget.SPEAKER
                AppCategory.SYSTEM -> RouteTarget.SPEAKER
                AppCategory.OTHER -> RouteTarget.DEFAULT
            }

            result.add(
                AppAudioRule(
                    packageName = packageName,
                    appName = appLabel,
                    routeTarget = defaultRoute,
                    category = category,
                    isSystemApp = isSystem
                )
            )
        }

        return result.sortedWith(
            compareBy<AppAudioRule> { it.category.ordinal }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun classifyApp(packageName: String, label: String, info: ApplicationInfo): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerName = label.lowercase()

        val musicKeywords = listOf("music", "spotify", "audio", "sound", "radio", "podcast", "deezer", "gaana", "wynk", "jiosaavn", "pandora", "tidal", "apple.android.music", "soundcloud", "bandcamp", "shazam", "audiomack", "mp3", "walkman", "player")
        val callKeywords = listOf("phone", "dialer", "telecom", "whatsapp", "telegram", "meet", "teams", "zoom", "skype", "viber", "call", "messenger", "duo", "discord", "signal", "truecaller")
        val videoKeywords = listOf("youtube", "netflix", "primevideo", "disney", "twitch", "tiktok", "vlc", "mxplayer", "video", "hulu", "hotstar", "cinema")

        if (musicKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) return AppCategory.MUSIC
        if (callKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) return AppCategory.COMMUNICATION
        if (videoKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) return AppCategory.VIDEO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.category == ApplicationInfo.CATEGORY_GAME) return AppCategory.GAME
        if (lowerPkg.contains("game") || lowerName.contains("game")) return AppCategory.GAME
        if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return AppCategory.SYSTEM

        return AppCategory.OTHER
    }
}
