package com.example.safeher.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.safeher.data.model.LiveLocation
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import java.net.URLEncoder

private fun getHuaweiStaticMapUrl(location: LiveLocation, apiKey: String): String {

    Log.d("getHuaweiStaticMapUrl", "Latitude: ${location.location?.latitude} Longitude: ${location.location?.longitude} apiKey: $apiKey")

    val lat = location.location?.latitude
    val lon = location.location?.longitude
    val encodedApiKey = URLEncoder.encode(apiKey, "UTF-8")

    val marker = "color:blue|label:S|$lat,$lon"
    val encodedMarker = URLEncoder.encode(marker, "UTF-8")

    return "https://mapapi.cloud.huawei.com/mapApi/v1/mapService/getStaticMap" +
            "?location=$lat,$lon" +
            "&zoom=15" +
            "&width=600&height=400" +
            "&scale=2" +
            "&markers=$encodedMarker" +
            "&key=$encodedApiKey"
}

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
    val apiKey = "DgEDADECaluZSQg97ljDr5L7mWEB2dnpdZfnKhmW7KYY0XiK7B6arrz+T+QraG7a7a/ZFMlX1bksd7hahKFIp6lrxrjKZsSsYlFEiA=="

    if (location?.location == null) {
        StaticMapPreview(location = location, modifier = modifier)
        return
    }

    val mapImageUrl = remember(location) {
        getHuaweiStaticMapUrl(location, apiKey)
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
            AsyncImage(
                model = mapImageUrl,
                contentDescription = "Map preview of location",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // NEW: Make the map image clickable
                    .clickable {
                        val lat = location.location.latitude
                        val lon = location.location.longitude
                        // This opens the map to view the location pin
                        val intentUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri).apply {
                            setPackage("com.huawei.maps.app") // Use Petal Maps
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Petal Maps app not installed.", Toast.LENGTH_LONG).show()
                        }
                    }
            )

            // The "Directions" button can stay as it is
            ExtendedFloatingActionButton(
                onClick = {
                    val lat = location.location.latitude
                    val lon = location.location.longitude
                    // This intent specifically starts turn-by-turn navigation
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

@Composable
fun InteractiveHmsMap(
    location: LiveLocation?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // State to hold the map and marker objects once they are created
    var huaweiMap by remember { mutableStateOf<HuaweiMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

    val mapView = remember { MapView(context).apply { id = View.generateViewId() } }

    // This effect runs whenever the 'location' parameter changes
    LaunchedEffect(location) {
        val map = huaweiMap ?: return@LaunchedEffect
        val loc = location?.location ?: return@LaunchedEffect
        val newLatLng = LatLng(loc.latitude, loc.longitude)

        if (marker == null) {
            // If the marker doesn't exist yet, create it and store it
            marker = map.addMarker(
                MarkerOptions()
                    .position(newLatLng)
                    .title("Current Location")
            )
            // Also move the camera to the first location
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 16f))
        } else {
            // If the marker already exists, just update its position
            marker?.position = newLatLng
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(lifecycle, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(null) // Pass null for the bundle
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycle.addObserver(observer)

            val callback = OnMapReadyCallback { map ->
                Log.d("InteractiveHmsMap", "Map is ready.")
                // Map is ready, store it in our state variable so LaunchedEffect can use it
                huaweiMap = map
                // You can configure UI settings here if needed
                map.uiSettings.isZoomControlsEnabled = true
            }
            mapView.getMapAsync(callback)

            onDispose {
                lifecycle.removeObserver(observer)
                mapView.onDestroy()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    userId: String,
    mapViewModel: MapViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    // Collect all locations from the shared ViewModel
    val locationsUiState by mapViewModel.locationsUiState.collectAsState()

    var userLocation: LiveLocation? = null
    if (locationsUiState is UiState.Success) {
        userLocation = (locationsUiState as UiState.Success).data.find { it.userId == userId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = userLocation?.displayName ?: "Live Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Here we use the fully interactive map!
            InteractiveHmsMap(
                location = userLocation,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}