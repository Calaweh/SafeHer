package com.example.safeher.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.safeher.data.model.LiveLocation
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.delay

@Composable
fun MapPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading map...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StaticHmsMapPreview(
    location: LiveLocation?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapInitSuccess by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

//    LaunchedEffect(Unit) {
//        try {
//            MapsInitializer.initialize(context)
//            Log.d("HmsMapPreview", "HMS Maps initialized successfully")
//            mapInitSuccess = true
//        } catch (e: Exception) {
//            Log.e("HmsMapPreview", "Failed to initialize HMS Maps", e)
//        }
//    }

    if (!mapInitSuccess || location?.location == null) {
        StaticMapPreview(location = location, modifier = modifier)  // Assuming StaticMapPreview is defined
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            if (!mapReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            val mapView = remember { MapView(context).apply { id = View.generateViewId() } }

            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )

            DisposableEffect(lifecycle, mapView) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)

                try {
                    val callback = OnMapReadyCallback { huaweiMap ->
                        Log.d("HmsMapPreview", "OnMapReady called successfully")
                        mapReady = true

                        huaweiMap.uiSettings.isZoomControlsEnabled = false
                        huaweiMap.uiSettings.isScrollGesturesEnabled = false
                        huaweiMap.uiSettings.isZoomGesturesEnabled = false
                        huaweiMap.uiSettings.isRotateGesturesEnabled = false
                        huaweiMap.uiSettings.isTiltGesturesEnabled = false
                        huaweiMap.uiSettings.isMyLocationButtonEnabled = false

                        // Set camera position
                        huaweiMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.location.latitude, location.location.longitude),
                                15f
                            )
                        )
                        // Add marker
                        huaweiMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(location.location.latitude, location.location.longitude))
                                .title("Location")
                        )
                    }
                    mapView.getMapAsync(callback)
                    Log.d("HmsMapPreview", "MapView created and getMapAsync called")
                } catch (e: Exception) {
                    Log.e("HmsMapPreview", "Failed to create MapView", e)
                    mapInitSuccess = false
                }

                onDispose {
                    lifecycle.removeObserver(observer)
                    try {
                        mapView.onDestroy()
                        Log.d("HmsMapPreview", "MapView cleaned up")
                    } catch (e: Exception) {
                        Log.e("HmsMapPreview", "MapView cleanup failed", e)
                    }
                }
            }

            LaunchedEffect(Unit) {
                delay(5000)
                if (!mapReady) {
                    Log.e("HmsMapPreview", "Map ready timeout - falling back")
                    mapInitSuccess = false
                }
            }

            if (mapReady) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val lat = location.location.latitude
                        val lon = location.location.longitude
                        val intentUri = Uri.parse("petalmaps://navigation?daddr=$lat,$lon")
                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri).apply {
                            setPackage("com.huawei.maps.app")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Petal Maps app not installed.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    text = { Text("Directions") },
                    icon = { Icon(Icons.Outlined.Directions, contentDescription = "Directions") }
                )
            }
        }
    }
}