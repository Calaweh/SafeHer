package com.example.safeher.data.repository

import com.example.safeher.data.model.LocationSharingState
import com.example.safeher.data.model.SharingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSharingRepository @Inject constructor() {
    private val _sharingState = MutableStateFlow(LocationSharingState(SharingMode.IDLE))
    val sharingState = _sharingState.asStateFlow()

    private val _sharingStartedTrigger = MutableStateFlow<Long>(0L)
    val sharingStartedTrigger = _sharingStartedTrigger.asStateFlow()

    fun updateState(newState: LocationSharingState) {
        _sharingState.value = newState
    }

    fun resetState() {
        _sharingState.value = LocationSharingState()
    }

    fun triggerSharingStarted() {
        _sharingStartedTrigger.value = System.currentTimeMillis()
    }
}