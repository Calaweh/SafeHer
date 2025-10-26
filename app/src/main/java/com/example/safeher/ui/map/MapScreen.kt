package com.example.safeher.ui.map

//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.compose.*

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MapScreen(
//    viewModel: MapViewModel = hiltViewModel()
//) {
//    val friendLocations by viewModel.friendLocations.collectAsState()
//
//    val defaultCameraPosition = remember {
//        CameraPosition.fromLatLngZoom(LatLng(40.7128, -74.0060), 10f)
//    }
//    val cameraPositionState = rememberCameraPositionState {
//        position = defaultCameraPosition
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Friends' Locations") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = Color.White
//                )
//            )
//        }
//    ) { padding ->
//        Box(modifier = Modifier.padding(padding)) {
//            GoogleMap(
//                modifier = Modifier.fillMaxSize(),
//                cameraPositionState = cameraPositionState,
//                uiSettings = MapUiSettings(
//                    zoomControlsEnabled = true,
//                    compassEnabled = true
//                )
//            ) {
//                friendLocations.forEach { friendLocation ->
//                    // Make sure the location data is not null
//                    friendLocation.location?.let { geoPoint ->
//                        val position = LatLng(geoPoint.latitude, geoPoint.longitude)
//
//                        Marker(
//                            state = MarkerState(position = position),
//                            title = friendLocation.displayName,
//                            snippet = "Last updated: ${friendLocation.lastUpdated}"
//                        )
//                    }
//                }
//            }
//        }
//    }
//}