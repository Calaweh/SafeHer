package com.example.safeher.ui.resource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    suspend fun getGeminiApiKey(): String {
        remoteConfig.fetchAndActivate().await()
        return remoteConfig.getString("GEMINI_API_KEY")
    }
}
