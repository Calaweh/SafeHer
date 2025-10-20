package com.example.safeher.utils

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface ILocationProvider {
    fun getLocationUpdates(): Flow<Location>
}