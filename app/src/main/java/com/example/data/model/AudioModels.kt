package com.example.data.model

enum class RouteTarget(val displayName: String, val description: String) {
    BLUETOOTH("Bluetooth", "Stream audio to connected Bluetooth headset or speaker"),
    SPEAKER("Phone Speaker", "Play audio through the phone's built-in speaker"),
    DEFAULT("System Default", "Follow standard Android system audio routing")
}

enum class AppCategory(val label: String) {
    MUSIC("Music & Audio"),
    COMMUNICATION("Calls & Chat"),
    VIDEO("Video & Streaming"),
    GAME("Games"),
    SYSTEM("System"),
    OTHER("Other Apps")
}

enum class AudioFocusStatus(val label: String) {
    FOCUS_GAINED("Focus Held"),
    FOCUS_LOSS("Focus Lost"),
    FOCUS_LOSS_TRANSIENT("Focus Paused"),
    FOCUS_LOSS_DUCK("Audio Ducked"),
    IDLE("Inactive")
}

data class AppAudioRule(
    val packageName: String,
    val appName: String,
    val routeTarget: RouteTarget = RouteTarget.DEFAULT,
    val category: AppCategory = AppCategory.OTHER,
    val isSystemApp: Boolean = false,
    val hasAudioCapability: Boolean = true,
    val audioReason: String = "Media Playback"
)

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isConnected: Boolean = false,
    val isBonded: Boolean = true,
    val deviceType: String = "Bluetooth Audio",
    val isPreferred: Boolean = false
)

data class AudioStreamConfig(
    val isDualRoutingActive: Boolean = true,
    val musicTarget: RouteTarget = RouteTarget.BLUETOOTH,
    val callTarget: RouteTarget = RouteTarget.SPEAKER,
    val notificationTarget: RouteTarget = RouteTarget.SPEAKER,
    val otherAppsDefault: RouteTarget = RouteTarget.SPEAKER,
    val preferredBluetoothAddress: String? = null,
    val isServiceRunning: Boolean = false
)

data class AudioHardwareStatus(
    val isBluetoothA2dpConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isCommunicationDeviceSpeaker: Boolean = false,
    val currentCallState: String = "IDLE",
    val communicationDeviceName: String = "Default",
    val audioMode: String = "NORMAL",
    val audioFocusStatus: AudioFocusStatus = AudioFocusStatus.IDLE,
    val isSpeakerphoneForced: Boolean = false,
    val activeAudioRoute: RouteTarget = RouteTarget.DEFAULT,
    val audioAppsCount: Int = 0
)
