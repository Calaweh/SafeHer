package com.example.safeher.ui.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.datasource.LocationRemoteDataSource
import com.example.safeher.data.datasource.UserDataSource
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.LiveLocation
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.LocationSharingRepository
import com.example.safeher.utils.ILocationProvider
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationSharingRepository: LocationSharingRepository,
    private val userDataSource: UserDataSource,
    private val locationProvider: ILocationProvider
) : ViewModel() {

    private val _locationsUiState = MutableStateFlow<UiState<List<LiveLocation>>>(UiState.Loading)
    val locationsUiState: StateFlow<UiState<List<LiveLocation>>> = _locationsUiState.asStateFlow()

    private val _allFriends = MutableStateFlow<List<Friend>>(emptyList())

    private val _trackedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val trackedFriendIds: StateFlow<Set<String>> = _trackedFriendIds.asStateFlow()

    private val _selectedUserId = MutableStateFlow<String?>(null)
    val selectedUserId: StateFlow<String?> = _selectedUserId.asStateFlow()

    val friendLocations: StateFlow<List<LiveLocation>> = locationsUiState.map {
        if (it is UiState.Success) it.data else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendTrackingInfo: StateFlow<List<FriendTrackingInfo>> =
        combine(_allFriends, friendLocations) { allFriends, locations ->
            val sharingUserIds = locations.map { it.userId }.toSet()
            allFriends.map { friend ->
                FriendTrackingInfo(
                    friend = friend,
                    isSharingLocation = sharingUserIds.contains(friend.id)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userLocationFlow: Flow<LiveLocation?> = userDataSource.userState
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                flow {
                    val initialLocation = locationProvider.getLastKnownLocation()
                    Log.d("MapViewModel", "getLastKnownLocation() returned: $initialLocation")
                    if (initialLocation != null) {
                        emit(initialLocation)
                    }

                    locationProvider.getLocationUpdates().collect { newLocation ->
                        emit(newLocation)
                    }
                }
                    .map<Location, LiveLocation?> { location ->
                        LiveLocation(
                            userId = user.id,
                            displayName = "My Location",
                            imageUrl = user.imageUrl,
                            location = GeoPoint(location.latitude, location.longitude),
                            isSharing = true
                        )
                    }
                    .catch { e ->
                        Log.e("MapViewModel", "Error getting user's own location", e)
                        emit(null)
                    }
            }
        }

    val currentUserLocation: StateFlow<LiveLocation?> = userLocationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentLocations = mutableListOf<LiveLocation>()

    init {
        Log.d("MapViewModel", "MapViewModel INIT - Starting location observation")
        observeAllLocations()

        viewModelScope.launch {
            locationSharingRepository.sharingStartedTrigger
                .collect { timestamp ->
                    Log.d("MapViewModel", "Sharing started trigger received: $timestamp")
                    restartLocationObservation()
                }
        }
    }

    private fun restartLocationObservation() {
        Log.d("MapViewModel", "Restarting location observation due to sharing start")
        _locationsUiState.value = UiState.Loading
        currentLocations.clear()
        observeAllLocations()
    }

    fun updateTrackedFriends(friendIds: Set<String>) {
        _trackedFriendIds.value = friendIds
    }

    private fun observeAllLocations() {
        viewModelScope.launch {
            _locationsUiState.value = UiState.Loading
            currentLocations.clear()

            var lastLocation: Location? = null
            var attempts = 0
            while (lastLocation == null && attempts < 3) {
                lastLocation = withTimeoutOrNull(3000) {
                    locationProvider.getLastKnownLocation()
                }
                attempts++
                if (lastLocation == null) {
                    delay(1000)
                }
            }

            if (lastLocation != null) {
                updateMyLocation(lastLocation)
                _locationsUiState.value = UiState.Success(currentLocations.toList())
                Log.d("MapViewModel", "Used last known location on retry")
            } else {
                _locationsUiState.value = UiState.Empty
                Log.d("MapViewModel", "No last location after retries - waiting for updates")
            }

            locationProvider.getLocationUpdates()
                .catch { e ->
                    Log.e("MapViewModel", "Location Flow error: $e", e)
                    _locationsUiState.value = UiState.Error(e.message ?: "Location failed")
                }
                .onEach { location ->
                    updateMyLocation(location)
                    _locationsUiState.value = UiState.Success(currentLocations.toList())
                }
                .launchIn(this)
        }
    }

    fun updateSelectedUser(userId: String?) {
        _selectedUserId.value = userId
        Log.d("MapViewModel", "Selected user updated to: $userId")
    }

    fun getLocationForUser(userId: String): LiveLocation? {
        return when (val state = _locationsUiState.value) {
            is UiState.Success -> state.data.find { it.userId == userId }
            else -> null
        }
    }

    private fun updateMyLocation(location: Location) {
        val user = userDataSource.userState.value ?: return
        val liveLocation = LiveLocation(
            userId = user.id,
            displayName = "My Location",
            imageUrl = user.imageUrl,
            location = GeoPoint(location.latitude, location.longitude),
            isSharing = true
        )
        currentLocations.removeAll { it.userId == liveLocation.userId }
        currentLocations.add(liveLocation)
    }

}