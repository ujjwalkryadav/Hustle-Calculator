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

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SETUP_COMPLETE] ?: false
        }
        
    val userName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME]
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
}
