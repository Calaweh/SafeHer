package com.example.safeher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.safeher.utils.ILocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FallbackLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ILocationProvider {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
        Log.d("FallbackLocationProvider", "Using Android LocationManager")

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d("FallbackLocationProvider", "Location: ${location.latitude}, ${location.longitude}")
                trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {
                Log.d("FallbackLocationProvider", "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.w("FallbackLocationProvider", "Provider disabled: $provider")
            }
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000L,
                    10f,
                    locationListener,
                    Looper.getMainLooper()
                )
                Log.d("FallbackLocationProvider", "GPS location updates started")
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10000L,
                    10f,
                    locationListener,
                    Looper.getMainLooper()
                )
                Log.d("FallbackLocationProvider", "Network location updates started")
            }

            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastKnown?.let {
                Log.d("FallbackLocationProvider", "Sending last known location")
                trySend(it)
            }

        } catch (e: Exception) {
            Log.e("FallbackLocationProvider", "Failed to start location updates", e)
            close(e)
        }

        awaitClose {
            Log.d("FallbackLocationProvider", "Stopping location updates")
            locationManager.removeUpdates(locationListener)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            val gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            (gps ?: net)?.also {
                Log.d("FallbackLocationProvider", "Last known: ${it.latitude},${it.longitude}")
            } ?: run {
                Log.w("FallbackLocationProvider", "Last known null")
                null
            }
        } catch (e: SecurityException) {
            Log.e("FallbackLocationProvider", "Permission error", e)
            null
        }
    }
}