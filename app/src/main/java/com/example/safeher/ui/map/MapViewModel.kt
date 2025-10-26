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
import com.example.safeher.utils.ILocationProvider
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val locationDataSource: LocationRemoteDataSource,
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
                locationProvider.getLocationUpdates()
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

    init {
        observeAllLocations()
    }

    fun updateTrackedFriends(friendIds: Set<String>) {
        _trackedFriendIds.value = friendIds
    }

    private fun observeAllLocations() {
        viewModelScope.launch {
            try {
                _locationsUiState.value = UiState.Loading

                val user = userDataSource.userState.first { it != null }
                    ?: run {
                        _locationsUiState.value = UiState.Success(emptyList())
                        return@launch
                    }

                friendRepository.getFriendsByUser(user.id)
                    .onEach { friendsData ->
                        _allFriends.value = friendsData.friends
                        if (_trackedFriendIds.value.isEmpty() && friendsData.friends.isNotEmpty()) {
                            _trackedFriendIds.value = friendsData.friends.map { it.id }.toSet()
                        }
                    }
                    .catch { e ->
                        Log.e("MapViewModel", "Error getting all friends", e)
                        _allFriends.value = emptyList()
                    }
                    .launchIn(viewModelScope)

                val friendsLocationsFlow: Flow<List<LiveLocation>> = _trackedFriendIds.flatMapLatest { trackedIds ->
                    if (trackedIds.isNotEmpty()) {
                        locationDataSource.getFriendsLocations(trackedIds.toList())
                    } else {
                        flowOf(emptyList())
                    }
                }.catch { e ->
                    Log.e("MapViewModel", "Error observing friend locations", e)
                    emit(emptyList())

                    _locationsUiState.value = UiState.Error("Failed to load friend locations.")
                }

                combine(userLocationFlow, friendsLocationsFlow) { myLocation, friendLocations ->
                    val combinedList = friendLocations.toMutableList()
                    myLocation?.let { combinedList.add(0, it) }
                    combinedList
                }.collect { finalLocationsList ->
                    _locationsUiState.value = UiState.Success(finalLocationsList)
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "An error occurred in observeAllLocations", e)
                _locationsUiState.value = UiState.Error(e.message)
            }
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
}