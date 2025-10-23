package com.example.safeher.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.safeher.data.model.LiveLocation
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MarkerOptions

@Composable
fun StaticHmsMapPreview(
    location: LiveLocation?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Fallback initialization (optional, prefer Application class)
    DisposableEffect(Unit) {
        MapsInitializer.initialize(context)
        onDispose { }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Check if we have a valid location to display
        if (location?.location != null) {
            val hmsLatLng = remember(location) {
                LatLng(location.location.latitude, location.location.longitude)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                // Huawei MapView wrapped in AndroidView
                val mapView = remember { MapView(context).apply { id = View.generateViewId() } }

                AndroidView(
                    factory = { mapView },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    update = { view ->
                        // Update logic (if needed) can go here
                    }
                )

                // Handle MapView lifecycle
                DisposableEffect(mapView) {
                    val callback = OnMapReadyCallback { huaweiMap ->
                        // Disable interactive features for static map
                        huaweiMap.uiSettings.isZoomControlsEnabled = false
                        huaweiMap.uiSettings.isScrollGesturesEnabled = false
                        huaweiMap.uiSettings.isZoomGesturesEnabled = false
                        huaweiMap.uiSettings.isRotateGesturesEnabled = false
                        huaweiMap.uiSettings.isTiltGesturesEnabled = false
                        huaweiMap.uiSettings.isMyLocationButtonEnabled = false

                        // Set camera position
                        huaweiMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(hmsLatLng, 15f)
                        )
                        // Add marker
                        huaweiMap.addMarker(
                            MarkerOptions()
                                .position(hmsLatLng)
                                .title("Location")
                        )
                    }
                    mapView.onCreate(Bundle())
                    mapView.getMapAsync(callback)

                    // Lifecycle cleanup
                    onDispose {
                        mapView.onPause()
                        mapView.onDestroy()
                    }
                }

                // Directions button for Petal Maps
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
        } else {
            // Fallback view when there is no location
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No location to display.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}