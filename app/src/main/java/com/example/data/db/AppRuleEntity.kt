package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_routing_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val routeTarget: String, // "BLUETOOTH", "SPEAKER", "DEFAULT"
    val category: String,
    val isCustom: Boolean = true
)

@Entity(tableName = "router_settings")
data class RouterSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isDualRoutingActive: Boolean = true,
    val musicTarget: String = "BLUETOOTH",
    val callTarget: String = "SPEAKER",
    val notificationTarget: String = "SPEAKER",
    val otherAppsDefault: String = "SPEAKER",
    val preferredBluetoothAddress: String? = null
)
