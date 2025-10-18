package com.example.safeher.data.utils

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface ILocationProvider {
    fun getLocationUpdates(): Flow<Location>
}