package com.example.safeher.data.service

import com.google.firebase.Firebase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.trace
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.safeher.R.xml as AppConfig

class FirestoreConfigurationService @Inject constructor() : ConfigurationService
{
    private val remoteConfig
        get() = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 0 }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(AppConfig.remote_config_defaults)
    }

    override suspend fun fetchConfiguration(): Boolean {
        return FirebasePerformance.getInstance().newTrace(FETCH_CONFIG_TRACE).trace {
            remoteConfig.fetchAndActivate().await()
        }
    }

    companion object {
        private const val SHOW_COMMUNITY_SCREEN_KEY = "show_community_screen" //Example only
        private const val FETCH_CONFIG_TRACE = "fetchConfig"
    }
}