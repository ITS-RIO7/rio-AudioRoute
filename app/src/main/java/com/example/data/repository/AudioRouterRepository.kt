package com.example.data.repository

import com.example.audio.AudioRoutingManager
import com.example.data.db.AppRuleDao
import com.example.data.db.AppRuleEntity
import com.example.data.db.RouterSettingsEntity
import com.example.data.model.AppAudioRule
import com.example.data.model.AppCategory
import com.example.data.model.AudioHardwareStatus
import com.example.data.model.AudioStreamConfig
import com.example.data.model.BluetoothDeviceInfo
import com.example.data.model.RouteTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AudioRouterRepository(
    private val appRuleDao: AppRuleDao,
    private val routingManager: AudioRoutingManager
) {
    val hardwareStatus: Flow<AudioHardwareStatus> = routingManager.hardwareStatus

    val routerSettings: Flow<AudioStreamConfig> = appRuleDao.getSettings().map { entity ->
        if (entity != null) {
            AudioStreamConfig(
                isDualRoutingActive = entity.isDualRoutingActive,
                musicTarget = RouteTarget.valueOf(entity.musicTarget),
                callTarget = RouteTarget.valueOf(entity.callTarget),
                notificationTarget = RouteTarget.valueOf(entity.notificationTarget),
                otherAppsDefault = RouteTarget.valueOf(entity.otherAppsDefault),
                preferredBluetoothAddress = entity.preferredBluetoothAddress
            )
        } else {
            AudioStreamConfig()
        }
    }.flowOn(Dispatchers.IO)

    val appRules: Flow<List<AppAudioRule>> = combine(
        appRuleDao.getAllRules()
    ) { dbRulesList ->
        val installed = routingManager.getInstalledAppsWithRules()
        val dbMap = dbRulesList.first().associateBy { it.packageName }

        installed.map { app ->
            val saved = dbMap[app.packageName]
            if (saved != null) {
                app.copy(
                    routeTarget = try {
                        RouteTarget.valueOf(saved.routeTarget)
                    } catch (_: Exception) {
                        app.routeTarget
                    }
                )
            } else {
                app
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateAppRule(packageName: String, appName: String, category: String, target: RouteTarget) {
        withContext(Dispatchers.IO) {
            appRuleDao.insertRule(
                AppRuleEntity(
                    packageName = packageName,
                    appName = appName,
                    routeTarget = target.name,
                    category = category,
                    isCustom = true
                )
            )
        }
    }

    suspend fun setAllAppsTarget(allApps: List<AppAudioRule>, target: RouteTarget) {
        withContext(Dispatchers.IO) {
            val targets = allApps.map {
                AppRuleEntity(
                    packageName = it.packageName,
                    appName = it.appName,
                    routeTarget = target.name,
                    category = it.category.name,
                    isCustom = true
                )
            }
            appRuleDao.insertRules(targets)
        }
    }

    suspend fun applyBatchCategoryTarget(category: AppCategory, target: RouteTarget, allApps: List<AppAudioRule>) {
        withContext(Dispatchers.IO) {
            val targets = allApps.filter { it.category == category }.map {
                AppRuleEntity(
                    packageName = it.packageName,
                    appName = it.appName,
                    routeTarget = target.name,
                    category = it.category.name,
                    isCustom = true
                )
            }
            appRuleDao.insertRules(targets)
        }
    }

    suspend fun saveSettings(config: AudioStreamConfig) {
        withContext(Dispatchers.IO) {
            appRuleDao.saveSettings(
                RouterSettingsEntity(
                    id = 1,
                    isDualRoutingActive = config.isDualRoutingActive,
                    musicTarget = config.musicTarget.name,
                    callTarget = config.callTarget.name,
                    notificationTarget = config.notificationTarget.name,
                    otherAppsDefault = config.otherAppsDefault.name,
                    preferredBluetoothAddress = config.preferredBluetoothAddress
                )
            )
            withContext(Dispatchers.Main) {
                routingManager.applyRoutingConfig(config)
            }
        }
    }

    fun getPairedBluetoothDevices(): List<BluetoothDeviceInfo> {
        return routingManager.getPairedBluetoothDevices()
    }

    fun refreshStatus() {
        routingManager.refreshAudioStatus()
    }
}
