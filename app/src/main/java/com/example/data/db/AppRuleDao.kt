package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_routing_rules")
    fun getAllRules(): Flow<List<AppRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AppRuleEntity>)

    @Query("DELETE FROM app_routing_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("SELECT * FROM router_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<RouterSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: RouterSettingsEntity)
}
