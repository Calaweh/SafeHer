package com.example.safeher.ui.checkin

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.LiveLocation
import com.example.safeher.data.model.LocationSharingState
import com.example.safeher.data.model.SharingMode
import com.example.safeher.ui.map.MapViewModel
import com.example.safeher.ui.map.StaticMapPreview
import com.example.safeher.ui.theme.SafeHerTheme
import com.google.accompanist.permissions.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSharingSection(
    sharingState: LocationSharingState,
    friends: List<Friend>,
    arePermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartInstant: (Long, List<String>) -> Unit,
    onStartDelayed: (Long, List<String>) -> Unit,
    onStop: () -> Unit
) {
    var showTimerDialog by remember { mutableStateOf(false) }
    var showFriendDialog by remember { mutableStateOf(false) }
    var shareMode by remember { mutableStateOf<ShareMode?>(null) }
    var selectedDuration by remember { mutableLongStateOf(0L) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (sharingState.mode) {
                SharingMode.IDLE -> MaterialTheme.colorScheme.surface
                SharingMode.COUNTDOWN -> MaterialTheme.colorScheme.tertiaryContainer
                SharingMode.SHARING -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Share Location Continuously",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = when (sharingState.mode) {
                        SharingMode.SHARING -> MaterialTheme.colorScheme.primary
                        SharingMode.COUNTDOWN -> MaterialTheme.colorScheme.tertiary
                        SharingMode.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            when (sharingState.mode) {
                SharingMode.IDLE -> {
                    Text(
                        text = "Share your live location with emergency contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            { if (arePermissionsGranted) {
                                shareMode = ShareMode.INSTANT
                                showTimerDialog = true
                            } else {
                                onRequestPermissions()
                            }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Now")
                        }

                        OutlinedButton(
                            onClick = {
                                if (arePermissionsGranted) {
                                    shareMode = ShareMode.DELAYED
                                    showTimerDialog = true
                                } else {
                                    onRequestPermissions()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Later")
                        }
                    }
                }

                SharingMode.COUNTDOWN -> {
                    val minutes = (sharingState.timeLeftInMillis / 60000).toInt()
                    val seconds = ((sharingState.timeLeftInMillis % 60000) / 1000).toInt()
                    val isAboutToStart = sharingState.timeLeftInMillis <= 5000 // 5 seconds or less

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isAboutToStart) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAboutToStart) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAboutToStart) {
                                    "Starting location sharing..."
                                } else {
                                    "Scheduled to share location"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAboutToStart) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = if (isAboutToStart) {
                                    "Starting in ${seconds}s"
                                } else if (minutes > 0) {
                                    "Starting in ${minutes}m ${seconds}s"
                                } else {
                                    "Starting in ${seconds}s"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isAboutToStart
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAboutToStart) "Starting..." else "Cancel Schedule")
                    }
                }

                SharingMode.SHARING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sharing location live",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Stops in ${"%.2f".format(sharingState.timeLeftInMillis / 60000.0)} minutes (${sharingState.timeLeftInMillis / 60000}m ${(sharingState.timeLeftInMillis % 60000) / 1000}s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Sharing")
                    }
                }
            }
        }
    }

    if (showTimerDialog && shareMode != null) {
        TimerSelectionDialog(
            mode = shareMode!!,
            onDismiss = { showTimerDialog = false },
            onConfirm = { minutes ->
                selectedDuration = minutes
                showTimerDialog = false
                showFriendDialog = true
            }
        )
    }

    if (showFriendDialog && shareMode != null) {
        Log.d("LocationSharingSection", "showing friend dialog")
        FriendSelectionDialog(
            friends = friends,
            onDismiss = { showFriendDialog = false },
            onConfirm = { selectedFriendIds ->

                when (shareMode) {
                    ShareMode.INSTANT -> onStartInstant(selectedDuration, selectedFriendIds)
                    ShareMode.DELAYED -> onStartDelayed(selectedDuration, selectedFriendIds)
                    null -> {}
                }
                showFriendDialog = false
            }
        )
    }
}

enum class ShareMode {
    INSTANT,
    DELAYED
}

@Composable
fun TimerSelectionDialog(
    mode: ShareMode,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedMinutes by remember { mutableLongStateOf(30L) }

    val title = when (mode) {
        ShareMode.INSTANT -> "Share Location Now"
        ShareMode.DELAYED -> "Schedule Location Share"
    }

    val description = when (mode) {
        ShareMode.INSTANT -> "How long do you want to share your location?"
        ShareMode.DELAYED -> "When should location sharing start?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(description)

                val timeOptions = if (mode == ShareMode.INSTANT) {
                    listOf(1L to "1 minutes", 30L to "30 minutes", 60L to "1 hour", 120L to "2 hours")
                } else {
                    listOf(1L to "1 minutes", 15L to "15 minutes", 30L to "30 minutes", 60L to "1 hour")
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeOptions.forEach { (minutes, label) ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = { selectedMinutes = minutes },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMinutes) }) {
                Text(if (mode == ShareMode.INSTANT) "Start Sharing" else "Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

//@Composable
//private fun FriendsMapView(
//    locations: List<LiveLocation>,
//    modifier: Modifier = Modifier
//) {
//    val defaultCameraPosition = remember {
//        CameraPosition.fromLatLngZoom(LatLng(40.7128, -74.0060), 10f)
//    }
//    val cameraPositionState = rememberCameraPositionState {
//        position = defaultCameraPosition
//    }
//
//    // This makes the map UI self-contained
//    GoogleMap(
//        modifier = modifier,
//        cameraPositionState = cameraPositionState,
//        uiSettings = MapUiSettings(zoomControlsEnabled = false) // Disable zoom controls for a cleaner look in a small view
//    ) {
//        locations.forEach { friendLocation ->
//            friendLocation.location?.let { geoPoint ->
//                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
//                Marker(
//                    state = MarkerState(position = position),
//                    title = friendLocation.displayName,
//                    snippet = "Last updated: ${friendLocation.lastUpdated}"
//                )
//            }
//        }
//    }
//}

@Composable
fun CheckInScreen(
    checkInViewModel: CheckInViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val sharingState by checkInViewModel.sharingState.collectAsState()
    val friendLocations by mapViewModel.friendLocations.collectAsState()
    val friends by checkInViewModel.friends.collectAsState()

    CheckInScreenContent(
        sharingState = sharingState,
        friendLocations = friendLocations,
        friends = friends,
        onStartInstant = checkInViewModel::startInstantShare,
        onStartDelayed = checkInViewModel::startDelayedShare,
        onStop = checkInViewModel::stopSharing
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CheckInScreenContent(
    sharingState: LocationSharingState,
    friendLocations: List<LiveLocation>,
    friends: List<Friend>,
    onStartInstant: (Long, List<String>) -> Unit,
    onStartDelayed: (Long, List<String>) -> Unit,
    onStop: () -> Unit
) {
    val scrollState = rememberScrollState()

    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Log.d("CheckInScreenContent", "showing location sharing section")
            LocationSharingSection(
                sharingState = sharingState,
                arePermissionsGranted = permissionState.allPermissionsGranted,
                onRequestPermissions = { permissionState.launchMultiplePermissionRequest() },
                friends = friends,
                onStartInstant = onStartInstant,
                onStartDelayed = onStartDelayed,
                onStop = onStop
            )

            if (friendLocations.isNotEmpty()) {
                Log.d("CheckInScreenContent","showing map")
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Friend Location Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val firstFriendLocation = friendLocations.first()

                Log.d("CheckInScreenContent", "showing static map preview")
                StaticMapPreview(
                    location = firstFriendLocation,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
              // Quick Actions Section
//            QuickActionsSection()
//
//            // Emergency SOS Section
//            EmergencySOSSection()
//
//            // Safety Timer Section
//            SafetyTimerSection()
//
//            // Recent Check-ins Section
//            RecentCheckInsSection()
        }

}

//@Preview(name = "Check-In Screen - Idle State", showBackground = true)
//@Composable
//fun CheckInScreenIdlePreview() {
//    SafeHerTheme {
//        CheckInScreenContent(
//            sharingState = LocationSharingState(mode = SharingMode.IDLE),
//            onStartInstant = {},
//            onStartDelayed = {},
//            onStop = {}
//        )
//    }
//}

//@Preview(name = "Location Section - Idle", showBackground = true)
//@Composable
//fun LocationSharingSectionIdlePreview() {
//    SafeHerTheme {
//        LocationSharingSection(
//            sharingState = LocationSharingState(mode = SharingMode.IDLE),
//            arePermissionsGranted = false,
//            onRequestPermissions = {},
//            onStartInstant = {},
//            onStartDelayed = {},
//            onStop = {}
//        )
//    }
//}
//
//@Preview(name = "Location Section - Countdown", showBackground = true)
//@Composable
//fun LocationSharingSectionCountdownPreview() {
//    SafeHerTheme {
//        LocationSharingSection(
//            sharingState = LocationSharingState(
//                mode = SharingMode.COUNTDOWN,
//                timeLeftInMillis = 14, // Example time
//                totalDurationInMillis = 15
//            ),
//            arePermissionsGranted = false,
//            onRequestPermissions = {},
//            onStartInstant = {},
//            onStartDelayed = {},
//            onStop = {}
//        )
//    }
//}
//
//@Preview(name = "Location Section - Sharing", showBackground = true)
//@Composable
//fun LocationSharingSectionSharingPreview() {
//    SafeHerTheme {
//        LocationSharingSection(
//            sharingState = LocationSharingState(
//                mode = SharingMode.SHARING,
//                timeLeftInMillis = 28,
//                totalDurationInMillis = 30
//            ),
//            arePermissionsGranted = false,
//            onRequestPermissions = {},
//            onStartInstant = {},
//            onStartDelayed = {},
//            onStop = {}
//        )
//    }
//}

@Preview(name = "Timer Dialog - Instant Share")
@Composable
fun TimerSelectionDialogInstantPreview() {
    SafeHerTheme {
        TimerSelectionDialog(
            mode = ShareMode.INSTANT,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "Timer Dialog - Delayed Share")
@Composable
fun TimerSelectionDialogDelayedPreview() {
    SafeHerTheme {
        TimerSelectionDialog(
            mode = ShareMode.DELAYED,
            onDismiss = {},
            onConfirm = {}
        )
    }
}