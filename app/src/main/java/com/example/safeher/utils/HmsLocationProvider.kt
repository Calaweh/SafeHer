package com.example.safeher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.safeher.utils.ILocationProvider
import com.huawei.hms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HmsLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ILocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
        Log.d("HmsLocationProvider", "Getting location updates (HMS)")

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000L
            fastestInterval = 5000L
        }

        Log.d("HmsLocationProvider", "Starting location call back")

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d("HmsLocationProvider", "Location: ${location.latitude}, ${location.longitude}")
                    trySend(location)
                } ?: Log.w("HmsLocationProvider", "No location in result")
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Log.e("HmsLocationProvider", "Location unavailable")
                    close(Exception("Location unavailable"))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("HmsLocationProvider", "HMS location updates started successfully")
        } catch (e: Exception) {
            Log.e("HmsLocationProvider", "Failed to start HMS location updates", e)
            close(e)
        }

        awaitClose {
            Log.d("HmsLocationProvider", "Stopping location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        Log.d("HmsLocationProvider", "getLastKnownLocation() CALLED")
        try {
            val location = fusedLocationClient.lastLocation.awaitHms()
            Log.d("HmsLocationProvider", "getLastKnownLocation() SUCCESS: $location")
            location
        } catch (e: Exception) {
            Log.e("HmsLocationProvider", "getLastKnownLocation() FAILED", e)
            null
        }
    }
}

suspend fun <T> com.huawei.hmf.tasks.Task<T>.awaitHms(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel(CancellationException("Task cancelled"))
    }
    cont.invokeOnCancellation {
        cancel()
    }
}