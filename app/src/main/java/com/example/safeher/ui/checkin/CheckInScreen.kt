package com.example.safeher.ui.checkin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.LiveLocation
import com.example.safeher.data.model.LocationSharingState
import com.example.safeher.data.model.SharingMode
import com.example.safeher.ui.map.FriendTrackingInfo
import com.example.safeher.ui.map.FriendsLocationDialog
import com.example.safeher.ui.map.MapViewModel
import com.example.safeher.ui.map.StaticHmsMapPreview
import com.example.safeher.ui.map.StaticMapPreview
import com.example.safeher.ui.map.UiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationSharingSection(
    sharingState: LocationSharingState,
    friends: List<Friend>,
    permissionState: MultiplePermissionsState,
    arePermissionsGranted: Boolean,
    onStartInstant: (Long, List<String>) -> Unit,
    onStartDelayed: (Long, List<String>) -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    var showTimerDialog by remember { mutableStateOf(false) }
    var showFriendDialog by remember { mutableStateOf(false) }
    var shareMode by remember { mutableStateOf<ShareMode?>(null) }
    var selectedDuration by remember { mutableLongStateOf(0L) }
    var pendingAction by remember { mutableStateOf<ShareMode?>(null) }

    LaunchedEffect(arePermissionsGranted, pendingAction) {
        Log.d("LocationSharing", "Permission state changed: $arePermissionsGranted, Pending: $pendingAction")
        if (arePermissionsGranted && pendingAction != null) {
            Log.d("LocationSharing", "Permissions granted! Executing pending action: $pendingAction")
            shareMode = pendingAction
            showTimerDialog = true
            pendingAction = null
        }
    }

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
                            onClick = {
                                Log.d("LocationSharing", "Share Now CLICKED! Permissions: $arePermissionsGranted")
                                if (arePermissionsGranted) {
                                    shareMode = ShareMode.INSTANT
                                    showTimerDialog = true
                                } else {

                                    pendingAction = ShareMode.INSTANT
                                    permissionState.launchMultiplePermissionRequest()
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
                                Log.d("LocationSharing", "Share Later CLICKED! Permissions granted: $arePermissionsGranted")
                                if (arePermissionsGranted) {
                                    shareMode = ShareMode.DELAYED
                                    showTimerDialog = true
                                } else {

                                    pendingAction = ShareMode.DELAYED
                                    permissionState.launchMultiplePermissionRequest()
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

                    if (pendingAction != null && !arePermissionsGranted) {
                        Text(
                            text = "Please grant location permissions to continue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
            onDismiss = {
                showTimerDialog = false
                shareMode = null
            },
            onConfirm = { minutes ->
                selectedDuration = minutes
                showTimerDialog = false
                showFriendDialog = true
            }
        )
    }

    if (showFriendDialog && shareMode != null) {
        Log.d("LocationSharingSection", "Showing friend dialog for mode: $shareMode")
        FriendSelectionDialog(
            friends = friends,
            onDismiss = {
                showFriendDialog = false
                shareMode = null
            },
            onConfirm = { selectedFriendIds ->
                Log.d("LocationSharingSection", "Starting share: mode=$shareMode, duration=$selectedDuration, friends=$selectedFriendIds")
                when (shareMode) {
                    ShareMode.INSTANT -> onStartInstant(selectedDuration, selectedFriendIds)
                    ShareMode.DELAYED -> onStartDelayed(selectedDuration, selectedFriendIds)
                    null -> {}
                }
                showFriendDialog = false
                shareMode = null
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

fun Modifier.preventParentScroll() = this.pointerInput(Unit) {
    detectDragGestures { change, _ ->
        change.consume()
    }
}

@Composable
fun CheckInScreen(
    checkInViewModel: CheckInViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val sharingState by checkInViewModel.sharingState.collectAsState()
    val friends by checkInViewModel.friends.collectAsState()

    val locationsUiState by mapViewModel.locationsUiState.collectAsState()
    val friendLocations by mapViewModel.friendLocations.collectAsState()
    val currentUserLocation by mapViewModel.currentUserLocation.collectAsState()
    val friendTrackingInfo by mapViewModel.friendTrackingInfo.collectAsState()
    val trackedFriendIds by mapViewModel.trackedFriendIds.collectAsState()
    val selectedUserId by mapViewModel.selectedUserId.collectAsState()

    CheckInScreenContent(
        sharingState = sharingState,
        locationsUiState = locationsUiState,
        friendLocations = friendLocations,
        currentUserLocation = currentUserLocation,
        friends = friends,
        friendTrackingInfo = friendTrackingInfo,
        trackedFriendIds = trackedFriendIds,
        selectedUserId = selectedUserId,
        onUpdateTrackedFriends = mapViewModel::updateTrackedFriends,
        onUpdateSelectedUser = mapViewModel::updateSelectedUser,
        onStartInstant = checkInViewModel::startInstantShare,
        onStartDelayed = checkInViewModel::startDelayedShare,
        onStop = checkInViewModel::stopSharing
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CheckInScreenContent(
    sharingState: LocationSharingState,
    locationsUiState: UiState<List<LiveLocation>>,
    friendLocations: List<LiveLocation>,
    currentUserLocation: LiveLocation?,
    friends: List<Friend>,
    friendTrackingInfo: List<FriendTrackingInfo>,
    trackedFriendIds: Set<String>,
    selectedUserId: String?,
    onUpdateTrackedFriends: (Set<String>) -> Unit,
    onUpdateSelectedUser: (String?) -> Unit,
    onStartInstant: (Long, List<String>) -> Unit,
    onStartDelayed: (Long, List<String>) -> Unit,
    onStop: () -> Unit
) {
    var showFriendLocationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(Lifecycle.State.INITIALIZED) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleState = event.targetState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cpuAbi = remember {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        val is64BitArm = primaryAbi.startsWith("arm64") || primaryAbi.startsWith("aarch64")
        val is32BitArm = primaryAbi.startsWith("armeabi")
        val isX86 = primaryAbi.contains("x86")

        Log.d("CheckInScreenContent", "Primary ABI: $primaryAbi")
        Log.d("CheckInScreenContent", "All ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        Log.d("CheckInScreenContent", "Is ARM: ${is64BitArm || is32BitArm}, Is x86: $isX86")

        is64BitArm || is32BitArm
    }

    val isHmsAvailable = remember {
        val hmsResult = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(context)
        val available = hmsResult == ConnectionResult.SUCCESS
        Log.d("CheckInScreenContent", "HMS Available: $available (result code: $hmsResult)")
        available
    }

    val shouldUseHmsMap = cpuAbi && isHmsAvailable

    Log.d("CheckInScreenContent", "=== MAP SELECTION ===")
    Log.d("CheckInScreenContent", "CPU ABI is ARM: $cpuAbi")
    Log.d("CheckInScreenContent", "HMS Available: $isHmsAvailable")
    Log.d("CheckInScreenContent", "Will use HMS Map: $shouldUseHmsMap")
    Log.d("CheckInScreenContent", "====================")

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

    var actualPermissionsGranted by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {

            val granted = permissionsToRequest.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            actualPermissionsGranted = granted
            Log.d("CheckInScreenContent", "Lifecycle RESUMED - Manual permission check: $granted")
            Log.d("CheckInScreenContent", "Accompanist says: ${permissionState.allPermissionsGranted}")
        }
    }

    LaunchedEffect(Unit) {
        val granted = permissionsToRequest.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        actualPermissionsGranted = granted
        Log.d("CheckInScreenContent", "Initial permission check: $granted")

        if (!granted && !permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            actualPermissionsGranted = true
            Log.d("CheckInScreenContent", "Accompanist detected permissions granted")
        }
    }

    if (showFriendLocationDialog) {
        FriendsLocationDialog(
            friendsInfo = friendTrackingInfo,
            currentUserLocation = currentUserLocation,
            initiallySelectedId = selectedUserId,
            onDismiss = { showFriendLocationDialog = false },
            onConfirm = { selectedId ->
                onUpdateSelectedUser(selectedId)
                showFriendLocationDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Log.d("CheckInScreenContent", "showing location sharing section")
        LocationSharingSection(
            sharingState = sharingState,
            permissionState = permissionState,  // Passed here
            arePermissionsGranted = permissionState.allPermissionsGranted,
            friends = friends,
            onStartInstant = onStartInstant,
            onStartDelayed = onStartDelayed,
            onStop = onStop
        )

        when (locationsUiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Text(
                    text = locationsUiState.message ?: "Failed to load friend locations.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            is UiState.Success -> {
                val locations = locationsUiState.data
                if (locations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Location Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showFriendLocationDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = "Select Location",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Navigate")
                        }
                    }

                    // Show selected location or first location
                    val displayLocation = selectedUserId?.let { id ->
                        locations.find { it.userId == id }
                    } ?: locations.first()

                    if (selectedUserId != null) {
                        Text(
                            text = "Showing: ${displayLocation.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (shouldUseHmsMap) {
                        StaticHmsMapPreview(
                            location = displayLocation,
                            modifier = Modifier.padding(top = 8.dp).preventParentScroll()
                        )
                    } else {
                        StaticMapPreview(
                            location = displayLocation,
                            modifier = Modifier.padding(top = 8.dp).preventParentScroll()
                        )
                    }
                }
            }
        }
//        // Quick Actions Section
//        QuickActionsSection()
////
////      // Emergency SOS Section
//        EmergencySOSSection()
////
////      // Safety Timer Section
//        SafetyTimerSection()
////
////      // Recent Check-ins Section
//        RecentCheckInsSection()
    }
}

//@Preview(name = "Check-In Screen - Idle, No Friends Sharing", showBackground = true)
//@Composable
//fun CheckInScreenIdlePreview() {
//    SafeHerTheme {
//        CheckInScreenContent(
//            sharingState = LocationSharingState(mode = SharingMode.IDLE),
//            friendLocations = emptyList(),
//            friends = listOf(Friend(id = "1", displayName = "Jane Doe")),
//            friendTrackingInfo = emptyList(),
//            trackedFriendIds = emptySet(),
//            onUpdateTrackedFriends = {},
//            onStartInstant = { _, _ -> },
//            onStartDelayed = { _, _ -> },
//            onStop = {}
//        )
//    }
//}
//
//@Preview(name = "Check-In Screen - With Friend Location", showBackground = true)
//@Composable
//fun CheckInScreenWithLocationPreview() {
//    SafeHerTheme {
//        val mockFriend = Friend(id = "friend1", displayName = "Jane Doe")
//        val mockLocation = LiveLocation(
//            userId = "friend1",
//            displayName = "Jane Doe",
//            location = com.google.firebase.firestore.GeoPoint(40.7128, -74.0060) // New York City; assume this matches your model
//        )
//        val mockTrackingInfo = listOf(
//            FriendTrackingInfo(friend = mockFriend, isSharingLocation = true)
//        )
//
//        CheckInScreenContent(
//            sharingState = LocationSharingState(mode = SharingMode.IDLE),
//            friendLocations = listOf(mockLocation), // Provide a location to show the map
//            friends = listOf(mockFriend),
//            friendTrackingInfo = mockTrackingInfo,
//            trackedFriendIds = setOf("friend1"),
//            onUpdateTrackedFriends = {},
//            onStartInstant = { _, _ -> },
//            onStartDelayed = { _, _ -> },
//            onStop = {}
//        )
//    }
//}
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Preview(name = "Location Section - Idle", showBackground = true)
//@Composable
//fun LocationSharingSectionIdlePreview() {
//    SafeHerTheme {
//        val mockPermissions = rememberMultiplePermissionsState(permissions = emptyList())  // Mock for preview
//
//        LocationSharingSection(
//            sharingState = LocationSharingState(mode = SharingMode.IDLE),
//            permissionState = mockPermissions,  // Mock
//            friends = emptyList(),
//            arePermissionsGranted = true,
//            onStartInstant = { _, _ -> },
//            onStartDelayed = { _, _ -> },
//            onStop = {}
//        )
//    }
//}
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Preview(name = "Location Section - Countdown", showBackground = true)
//@Composable
//fun LocationSharingSectionCountdownPreview() {
//    SafeHerTheme {
//        val mockPermissions = rememberMultiplePermissionsState(permissions = emptyList())  // Mock for preview
//
//        LocationSharingSection(
//            sharingState = LocationSharingState(
//                mode = SharingMode.COUNTDOWN,
//                timeLeftInMillis = 14000, // 14 seconds
//                totalDurationInMillis = 15000
//            ),
//            permissionState = mockPermissions,  // Mock
//            friends = emptyList(),
//            arePermissionsGranted = true,
//            onStartInstant = { _, _ -> },
//            onStartDelayed = { _, _ -> },
//            onStop = {}
//        )
//    }
//}
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Preview(name = "Location Section - Sharing", showBackground = true)
//@Composable
//fun LocationSharingSectionSharingPreview() {
//    SafeHerTheme {
//        val mockPermissions = rememberMultiplePermissionsState(permissions = emptyList())  // Mock for preview
//
//        LocationSharingSection(
//            sharingState = LocationSharingState(
//                mode = SharingMode.SHARING,
//                timeLeftInMillis = 28000, // 28 seconds
//                totalDurationInMillis = 30000
//            ),
//            permissionState = mockPermissions,  // Mock
//            friends = emptyList(),
//            arePermissionsGranted = true,
//            onStartInstant = { _, _ -> },
//            onStartDelayed = { _, _ -> },
//            onStop = {}
//        )
//    }
//}
//
//@Preview(name = "Timer Dialog - Instant Share")
//@Composable
//fun TimerSelectionDialogInstantPreview() {
//    SafeHerTheme {
//        TimerSelectionDialog(
//            mode = ShareMode.INSTANT,
//            onDismiss = {},
//            onConfirm = {}
//        )
//    }
//}
//
//@Preview(name = "Timer Dialog - Delayed Share")
//@Composable
//fun TimerSelectionDialogDelayedPreview() {
//    SafeHerTheme {
//        TimerSelectionDialog(
//            mode = ShareMode.DELAYED,
//            onDismiss = {},
//            onConfirm = {}
//        )
//    }
//}