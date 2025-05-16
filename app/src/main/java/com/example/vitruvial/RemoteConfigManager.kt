package com.example.vitruvial

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteConfigManager {
    companion object {
        private const val TAG = "RemoteConfigManager"
        private const val CLAUDE_API_KEY = "claude_api_key"
        
        // Default values in case Remote Config fails
        private val defaults = mapOf(
            CLAUDE_API_KEY to ""
        )
        
        private val remoteConfig = Firebase.remoteConfig
        
        init {
            // Set minimum fetch interval for development
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 60 // 1 minute for testing
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values
            remoteConfig.setDefaultsAsync(defaults)
        }
        
        /**
         * Fetch the latest values from Remote Config
         * @return true if fetch and activation was successful
         */
        suspend fun fetchAndActivate(): Boolean {
            return try {
                val result = remoteConfig.fetchAndActivate().await()
                Log.d(TAG, "Remote config fetched and activated: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote config", e)
                false
            }
        }
        
        /**
         * Get Claude API key from Remote Config
         */
        fun getClaudeApiKey(): String {
            val remoteKey = remoteConfig.getString(CLAUDE_API_KEY)
            if (remoteKey.isNotEmpty()) {
                return remoteKey
            }
            
            // If Remote Config fails or key is empty, fall back to BuildConfig
            return BuildConfig.CLAUDE_API_KEY
        }
    }
} 