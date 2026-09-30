package com.example.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.AppAudioRule
import com.example.data.model.AppCategory
import com.example.data.model.AudioFocusStatus
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

    private val _audioFocusStatus = MutableStateFlow(AudioFocusStatus.IDLE)
    val audioFocusStatus: StateFlow<AudioFocusStatus> = _audioFocusStatus.asStateFlow()

    private var a2dpProfile: BluetoothProfile? = null
    private var headsetProfile: BluetoothProfile? = null

    private var isCallStateListening = false
    private var audioFocusRequestCompat: AudioFocusRequest? = null

    // Audio Focus listener to manage Android system audio focus
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val status = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusStatus.FOCUS_GAINED
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusStatus.FOCUS_LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusStatus.FOCUS_LOSS_TRANSIENT
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocusStatus.FOCUS_LOSS_DUCK
            else -> AudioFocusStatus.IDLE
        }
        _audioFocusStatus.value = status
        _hardwareStatus.value = _hardwareStatus.value.copy(audioFocusStatus = status)

        // When focus is re-gained, re-assert desired routing if forced
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN && _hardwareStatus.value.isSpeakerphoneForced) {
            forceCommunicationToSpeaker(true)
        }
    }

    // Audio Device Callback to monitor real-time headphone & Bluetooth connection changes
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val hasNewBt = addedDevices?.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            } ?: false

            refreshAudioStatus()

            // If Bluetooth was newly connected and speaker is forced for calls/apps, re-apply routing
            if (hasNewBt && _hardwareStatus.value.isSpeakerphoneForced) {
                forceCommunicationToSpeaker(true)
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioStatus()
        }
    }

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
        registerDeviceCallback()
        setupCallStateListener()
        refreshAudioStatus()
    }

    private fun registerDeviceCallback() {
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error registering AudioDeviceCallback: ${e.message}")
        }
    }

    private fun initBluetoothProfiles() {
        try {
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error initializing Bluetooth profiles: ${e.message}")
        }
    }

    /**
     * Request Audio Focus from Android AudioManager.
     * Manages audio focus so other apps or Bluetooth don't unexpectedly silence audio.
     */
    fun requestAudioFocus(forCommunication: Boolean = false): Boolean {
        try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val usage = if (forCommunication) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA
                val contentType = if (forCommunication) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC
                val focusGain = if (forCommunication) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT else AudioManager.AUDIOFOCUS_GAIN

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build()

                val request = AudioFocusRequest.Builder(focusGain)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()

                audioFocusRequestCompat = request
                audioManager.requestAudioFocus(request)
            } else {
                val streamType = if (forCommunication) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    streamType,
                    if (forCommunication) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT else AudioManager.AUDIOFOCUS_GAIN
                )
            }

            val isGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (isGranted) {
                _audioFocusStatus.value = AudioFocusStatus.FOCUS_GAINED
                _hardwareStatus.value = _hardwareStatus.value.copy(audioFocusStatus = AudioFocusStatus.FOCUS_GAINED)
            }
            return isGranted
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error requesting audio focus: ${e.message}")
            return false
        }
    }

    /**
     * Abandon Audio Focus when service or routing is released.
     */
    fun abandonAudioFocus(): Boolean {
        try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequestCompat?.let {
                    audioManager.abandonAudioFocusRequest(it)
                } ?: AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
            _audioFocusStatus.value = AudioFocusStatus.IDLE
            _hardwareStatus.value = _hardwareStatus.value.copy(audioFocusStatus = AudioFocusStatus.IDLE)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Error abandoning audio focus: ${e.message}")
            return false
        }
    }

    fun refreshAudioStatus() {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasBtA2dp = outputDevices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
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

        val currentForced = _hardwareStatus.value.isSpeakerphoneForced
        val activeTarget = when {
            isSpeakerForCalls || currentForced -> RouteTarget.SPEAKER
            hasBtA2dp -> RouteTarget.BLUETOOTH
            else -> RouteTarget.DEFAULT
        }

        _hardwareStatus.value = _hardwareStatus.value.copy(
            isBluetoothA2dpConnected = hasBtA2dp,
            connectedDeviceName = connectedBtName,
            isCommunicationDeviceSpeaker = isSpeakerForCalls,
            communicationDeviceName = commDeviceName,
            audioMode = modeStr,
            activeAudioRoute = activeTarget
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

        // Apply call & voice routing specifically to Phone Speaker if selected
        if (config.callTarget == RouteTarget.SPEAKER) {
            forceCommunicationToSpeaker(true)
        } else if (config.callTarget == RouteTarget.BLUETOOTH) {
            forceCommunicationToBluetooth()
        } else {
            resetRoutingToDefault()
        }

        refreshAudioStatus()
    }

    /**
     * CRITICAL FIX FOR BLUETOOTH OVERRIDE:
     * When Bluetooth is connected, Android routes all audio to Bluetooth unless:
     * 1. audioManager.mode is explicitly set to MODE_IN_COMMUNICATION.
     * 2. On API 31+, setCommunicationDevice(TYPE_BUILTIN_SPEAKER) is called.
     * 3. On pre-31, isSpeakerphoneOn is set to true.
     */
    fun forceCommunicationToSpeaker(force: Boolean) {
        try {
            if (force) {
                // Setting MODE_IN_COMMUNICATION is required for Android to honor speaker redirection over Bluetooth
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        val success = audioManager.setCommunicationDevice(speaker)
                        Log.d("AudioRoutingManager", "setCommunicationDevice speaker: $success")
                    }
                }

                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                _hardwareStatus.value = _hardwareStatus.value.copy(
                    isSpeakerphoneForced = true,
                    isCommunicationDeviceSpeaker = true,
                    activeAudioRoute = RouteTarget.SPEAKER
                )
            } else {
                resetRoutingToDefault()
            }
            refreshAudioStatus()
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Failed to force speaker: ${e.message}")
        }
    }

    fun forceCommunicationToBluetooth() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                val btDev = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (btDev != null) {
                    audioManager.setCommunicationDevice(btDev)
                }
            } else {
                audioManager.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
            }
            _hardwareStatus.value = _hardwareStatus.value.copy(
                isSpeakerphoneForced = false,
                activeAudioRoute = RouteTarget.BLUETOOTH
            )
            refreshAudioStatus()
        } catch (e: Exception) {
            Log.e("AudioRoutingManager", "Failed to force bluetooth: ${e.message}")
        }
    }

    fun resetRoutingToDefault() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL

            _hardwareStatus.value = _hardwareStatus.value.copy(
                isSpeakerphoneForced = false,
                activeAudioRoute = RouteTarget.DEFAULT
            )
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
            // Auto enforce speaker for incoming/active call so it rings on phone speaker
            forceCommunicationToSpeaker(true)
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            refreshAudioStatus()
        }
    }

    /**
     * CRITICAL USER REQUIREMENT:
     * "app list me co hi app do Jo audio provide karate hai"
     * Returns ONLY apps that actually provide or produce audio / sound:
     * - Music players, audio streaming services (Spotify, YouTube Music, Gaana, Wynk, SoundCloud, etc.)
     * - Video players and streaming services with audio (YouTube, Netflix, VLC, MX Player, etc.)
     * - Calling and voice apps (WhatsApp, Phone, Telegram, Zoom, Meet, Teams, Skype, etc.)
     * - Games that produce sound
     * - Filter out non-audio tools, launchers, calculators, settings, system daemons, etc.
     */
    fun getInstalledAppsWithRules(onlyAudioApps: Boolean = true): List<AppAudioRule> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppAudioRule>()

        for (info in installedApps) {
            val packageName = info.packageName
            val appLabel = pm.getApplicationLabel(info).toString()
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // 1. Must be a user-launchable app or have explicit media capabilities
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val hasLaunchIntent = launchIntent != null

            // 2. Classify audio capability
            val (isAudioProvider, category, audioReason) = evaluateAudioProvider(pm, packageName, appLabel, info)

            // If user requested only audio apps, filter out non-audio apps
            if (onlyAudioApps) {
                if (!isAudioProvider) continue
                // Don't show system daemons that aren't launchable
                if (isSystem && !hasLaunchIntent && category != AppCategory.COMMUNICATION) continue
            } else {
                // If not only audio apps, skip non-launchable background services
                if (!hasLaunchIntent && isSystem) continue
            }

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
                    isSystemApp = isSystem,
                    hasAudioCapability = isAudioProvider,
                    audioReason = audioReason
                )
            )
        }

        _hardwareStatus.value = _hardwareStatus.value.copy(audioAppsCount = result.size)

        return result.sortedWith(
            compareBy<AppAudioRule> { it.category.ordinal }
                .thenBy { it.appName.lowercase() }
        )
    }

    private data class AppAudioEvaluation(
        val isAudioProvider: Boolean,
        val category: AppCategory,
        val reason: String
    )

    private fun evaluateAudioProvider(
        pm: PackageManager,
        packageName: String,
        label: String,
        info: ApplicationInfo
    ): AppAudioEvaluation {
        val lowerPkg = packageName.lowercase()
        val lowerName = label.lowercase()

        // Explicit blacklisted non-audio system utilities
        val nonAudioKeywords = listOf(
            "calculator", "keyboard", "inputmethod", "wallpaper", "calendar",
            "contacts.sync", "documentsui", "packageinstaller", "permissioncontroller",
            "carrierconfig", "settingsprovider", "backup", "shell", "captiveportallogin"
        )
        if (nonAudioKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppAudioEvaluation(false, AppCategory.OTHER, "Utility (No Audio)")
        }

        // 1. Music & Audio Streaming
        val musicKeywords = listOf(
            "music", "spotify", "audio", "sound", "radio", "podcast", "deezer", "gaana",
            "wynk", "jiosaavn", "pandora", "tidal", "apple.android.music", "soundcloud",
            "bandcamp", "shazam", "audiomack", "mp3", "walkman", "player", "audioplayer",
            "poweramp", "aimp", "resso", "fm", "boomplay", "tunein", "audible", "pocketcasts"
        )
        if (musicKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppAudioEvaluation(true, AppCategory.MUSIC, "🎵 Music & Audio Streaming")
        }

        // 2. Calling & Voice Communication
        val callKeywords = listOf(
            "phone", "dialer", "telecom", "whatsapp", "telegram", "meet", "teams", "zoom",
            "skype", "viber", "call", "messenger", "duo", "discord", "signal", "truecaller",
            "talk", "voice", "voip", "imo"
        )
        if (callKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppAudioEvaluation(true, AppCategory.COMMUNICATION, "📞 Voice Calls & VoIP")
        }

        // 3. Video & Media Streaming
        val videoKeywords = listOf(
            "youtube", "netflix", "primevideo", "disney", "twitch", "tiktok", "vlc",
            "mxplayer", "video", "hulu", "hotstar", "cinema", "stremio", "tubi", "crunchyroll",
            "plex", "reels", "mediaplayer"
        )
        if (videoKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppAudioEvaluation(true, AppCategory.VIDEO, "🎬 Video & Media Player")
        }

        // 4. Android App Category (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (info.category) {
                ApplicationInfo.CATEGORY_AUDIO -> {
                    return AppAudioEvaluation(true, AppCategory.MUSIC, "🎵 Android Audio Category")
                }
                ApplicationInfo.CATEGORY_VIDEO -> {
                    return AppAudioEvaluation(true, AppCategory.VIDEO, "🎬 Android Video Category")
                }
                ApplicationInfo.CATEGORY_GAME -> {
                    return AppAudioEvaluation(true, AppCategory.GAME, "🎮 Game Audio")
                }
            }
        }

        if (lowerPkg.contains("game") || lowerName.contains("game")) {
            return AppAudioEvaluation(true, AppCategory.GAME, "🎮 Game Audio")
        }

        // 5. Inspect package permissions for audio playback / recording
        try {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requested = pkgInfo.requestedPermissions
            if (requested != null) {
                val hasRecordAudio = requested.contains("android.permission.RECORD_AUDIO")
                val hasModifyAudio = requested.contains("android.permission.MODIFY_AUDIO_SETTINGS")
                val hasMediaPlayback = requested.any { it.contains("MEDIA_PLAYBACK") || it.contains("AUDIO") }

                if (hasRecordAudio && hasModifyAudio) {
                    return AppAudioEvaluation(true, AppCategory.COMMUNICATION, "📞 Communication & Audio Permissions")
                }
                if (hasMediaPlayback || hasModifyAudio) {
                    return AppAudioEvaluation(true, AppCategory.MUSIC, "🎵 Audio Playback Capability")
                }
            }
        } catch (_: Exception) {}

        // Fallback: Not identified as an audio provider
        return AppAudioEvaluation(false, AppCategory.OTHER, "General Application")
    }
}
