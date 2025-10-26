package com.example.safeher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.huawei.hmf.tasks.Task
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationAvailability
import com.huawei.hms.location.LocationCallback
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationResult
import com.huawei.hms.location.LocationServices
import com.huawei.hms.location.LocationSettingsRequest
import com.huawei.hms.location.LocationSettingsResponse
import com.huawei.hms.location.LocationSettingsStates
import com.huawei.hms.location.LocationSettingsStatusCodes
import com.huawei.hms.location.SettingsClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitHms(): T = suspendCancellableCoroutine { cont ->
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

class HmsLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ILocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient: SettingsClient =
        LocationServices.getSettingsClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
        Log.d("HmsLocationProvider", "getting location updates")

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000L
            fastestInterval = 5000L
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        try {
            val response: LocationSettingsResponse = settingsClient.checkLocationSettings(builder.build()).awaitHms()
            val states: LocationSettingsStates = response.locationSettingsStates
            if (!states.isLocationUsable) {
                close(Exception("Location services unavailable"))
                return@callbackFlow
            }
        } catch (e: Exception) {
            val statusCode = (e as? com.huawei.hms.common.ApiException)?.statusCode
            if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                Log.e("HmsLocationProvider", "Location settings resolution required")

            }
            close(e)
            return@callbackFlow
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
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

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            Log.d("HmsLocationProvider", "stopping location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}