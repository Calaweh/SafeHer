package com.example.safeher.utils

//import android.annotation.SuppressLint
//import android.content.Context
//import android.location.Location
//import android.os.Looper
//import android.util.Log
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import javax.inject.Inject
//
//class GmsLocationProvider @Inject constructor(
//    @ApplicationContext private val context: Context
//) : ILocationProvider {
//
//    private val fusedLocationClient: FusedLocationProviderClient =
//        LocationServices.getFusedLocationProviderClient(context)
//
//    @SuppressLint("MissingPermission")
//    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
//
//        Log.d("GmsLocationProvider", "getting location updates")
//        val locationRequest = LocationRequest.Builder(
//            Priority.PRIORITY_HIGH_ACCURACY,
//            10000L
//        ).apply {
//            setMinUpdateIntervalMillis(5000L)
//        }.build()
//
//        val locationCallback = object : LocationCallback() {
//            override fun onLocationResult(result: LocationResult) {
//                result.lastLocation?.let { location ->
//                    trySend(location)
//                }
//            }
//        }
//
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )
//
//        awaitClose {
//            fusedLocationClient.removeLocationUpdates(locationCallback)
//        }
//    }
//}