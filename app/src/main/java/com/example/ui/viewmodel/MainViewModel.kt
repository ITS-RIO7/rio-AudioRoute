package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRoutingManager
import com.example.audio.AudioTestPlayer
import com.example.data.db.AppDatabase
import com.example.data.model.AppAudioRule
import com.example.data.model.AppCategory
import com.example.data.model.AudioHardwareStatus
import com.example.data.model.AudioStreamConfig
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.RouteTarget
import com.example.data.repository.AudioRouterRepository
import com.example.service.AudioRouterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AudioStreamConfig = AudioStreamConfig(),
    val hardwareStatus: AudioHardwareStatus = AudioHardwareStatus(),
    val bluetoothDevices: List<BluetoothDeviceInfo> = emptyList(),
    val appRules: List<AppAudioRule> = emptyList(),
    val filteredApps: List<AppAudioRule> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory? = null,
    val isMusicTestPlaying: Boolean = false,
    val isCallToneTestPlaying: Boolean = false,
    val isServiceRunning: Boolean = false,
    val selectedTab: Int = 0 // 0: Dashboard, 1: Apps, 2: Bluetooth Devices
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val routingManager = AudioRoutingManager(application)
    val repository = AudioRouterRepository(db.appRuleDao(), routingManager)
    val testPlayer = AudioTestPlayer(application, routingManager)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDeviceInfo>> = _bluetoothDevices.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(AudioRouterService.isRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.routerSettings,
        repository.hardwareStatus,
        repository.appRules,
        _searchQuery,
        _selectedCategory,
        _bluetoothDevices,
        testPlayer.isMusicPlaying,
        testPlayer.isCallTonePlaying,
        _selectedTab,
        _isServiceRunning
    ) { params ->
        val settings = params[0] as AudioStreamConfig
        val hardware = params[1] as AudioHardwareStatus
        val allApps = params[2] as List<AppAudioRule>
        val query = params[3] as String
        val cat = params[4] as? AppCategory
        val btDevices = params[5] as List<BluetoothDeviceInfo>
        val isMusicTest = params[6] as Boolean
        val isCallTest = params[7] as Boolean
        val tab = params[8] as Int
        val serviceRunning = params[9] as Boolean

        val filtered = allApps.filter { rule ->
            val matchesQuery = query.isEmpty() ||
                rule.appName.contains(query, ignoreCase = true) ||
                rule.packageName.contains(query, ignoreCase = true)
            val matchesCategory = cat == null || rule.category == cat
            matchesQuery && matchesCategory
        }

        MainUiState(
            settings = settings.copy(isServiceRunning = serviceRunning),
            hardwareStatus = hardware,
            bluetoothDevices = btDevices,
            appRules = allApps,
            filteredApps = filtered,
            searchQuery = query,
            selectedCategory = cat,
            isMusicTestPlaying = isMusicTest,
            isCallToneTestPlaying = isCallTest,
            isServiceRunning = serviceRunning,
            selectedTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        refreshBluetoothDevices()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: AppCategory?) {
        _selectedCategory.value = category
    }

    fun refreshBluetoothDevices() {
        _bluetoothDevices.value = repository.getPairedBluetoothDevices()
        repository.refreshStatus()
        _isServiceRunning.value = AudioRouterService.isRunning
    }

    fun toggleDualRouting() {
        val current = uiState.value.settings
        val updated = current.copy(isDualRoutingActive = !current.isDualRoutingActive)
        viewModelScope.launch {
            repository.saveSettings(updated)
            if (updated.isDualRoutingActive && !_isServiceRunning.value) {
                startRoutingService()
            }
        }
    }

    fun setMusicRoute(target: RouteTarget) {
        val current = uiState.value.settings
        viewModelScope.launch {
            repository.saveSettings(current.copy(musicTarget = target))
        }
    }

    fun setCallRoute(target: RouteTarget) {
        val current = uiState.value.settings
        viewModelScope.launch {
            repository.saveSettings(current.copy(callTarget = target))
        }
    }

    fun setNotificationRoute(target: RouteTarget) {
        val current = uiState.value.settings
        viewModelScope.launch {
            repository.saveSettings(current.copy(notificationTarget = target))
        }
    }

    fun selectBluetoothDevice(device: BluetoothDeviceInfo) {
        val current = uiState.value.settings
        viewModelScope.launch {
            repository.saveSettings(current.copy(preferredBluetoothAddress = device.address))
            refreshBluetoothDevices()
        }
    }

    fun updateAppRule(packageName: String, appName: String, category: String, target: RouteTarget) {
        viewModelScope.launch {
            repository.updateAppRule(packageName, appName, category, target)
        }
    }

    fun batchSetCategory(category: AppCategory, target: RouteTarget) {
        val allApps = uiState.value.appRules
        viewModelScope.launch {
            repository.applyBatchCategoryTarget(category, target, allApps)
        }
    }

    fun toggleService() {
        if (_isServiceRunning.value) {
            stopRoutingService()
        } else {
            startRoutingService()
        }
    }

    fun startRoutingService() {
        val context = getApplication<Application>()
        val intent = Intent(context, AudioRouterService::class.java).apply {
            action = AudioRouterService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _isServiceRunning.value = true
    }

    fun stopRoutingService() {
        val context = getApplication<Application>()
        val intent = Intent(context, AudioRouterService::class.java).apply {
            action = AudioRouterService.ACTION_STOP
        }
        context.startService(intent)
        _isServiceRunning.value = false
    }

    fun playMusicTest() {
        testPlayer.playMusicTest()
    }

    fun playCallTest() {
        testPlayer.playCallTest()
    }

    fun stopAllTests() {
        testPlayer.stopMusicTest()
        testPlayer.stopCallTest()
    }

    fun openBluetoothSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openSoundSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        testPlayer.release()
    }
}
