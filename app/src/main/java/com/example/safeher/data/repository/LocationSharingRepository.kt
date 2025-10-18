package com.example.safeher.data.repository

import com.example.safeher.data.model.LocationSharingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSharingRepository @Inject constructor() {
    private val _sharingState = MutableStateFlow(LocationSharingState())
    val sharingState = _sharingState.asStateFlow()

    fun updateState(newState: LocationSharingState) {
        _sharingState.value = newState
    }
}