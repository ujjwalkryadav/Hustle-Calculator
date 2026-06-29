package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    private val USER_NAME = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
    private val CUSTOM_CATEGORIES = androidx.datastore.preferences.core.stringPreferencesKey("custom_categories")

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SETUP_COMPLETE] ?: false
        }
        
    val userName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME]
        }
        
    val customCategories: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val catsStr = preferences[CUSTOM_CATEGORIES] ?: ""
            if (catsStr.isEmpty()) emptyList() else catsStr.split(",")
        }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SETUP_COMPLETE] = complete
        }
    }
    
    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }
    
    suspend fun addCustomCategory(category: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_CATEGORIES] ?: ""
            val list = if (current.isEmpty()) mutableListOf() else current.split(",").toMutableList()
            if (!list.contains(category)) {
                list.add(category)
                preferences[CUSTOM_CATEGORIES] = list.joinToString(",")
            }
        }
    }
    
    suspend fun removeCustomCategory(category: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_CATEGORIES] ?: ""
            val list = if (current.isEmpty()) mutableListOf() else current.split(",").toMutableList()
            if (list.contains(category)) {
                list.remove(category)
                preferences[CUSTOM_CATEGORIES] = list.joinToString(",")
            }
        }
    }
}
