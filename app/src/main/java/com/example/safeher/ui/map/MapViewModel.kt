package com.example.safeher.ui.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.datasource.LocationRemoteDataSource
import com.example.safeher.data.datasource.UserDataSource
import com.example.safeher.data.model.LiveLocation
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.utils.ILocationProvider
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    private val _friendLocations = MutableStateFlow<List<LiveLocation>>(emptyList())
    val friendLocations: StateFlow<List<LiveLocation>> = _friendLocations.asStateFlow()

    private val userLocationFlow: Flow<LiveLocation?> = userDataSource.userState
        .flatMapLatest { user ->
            if (user == null) {
                // If the user is null, emit a null value. This now matches the Flow's type.
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
                        // This is now valid because the Flow's type is Flow<LiveLocation?>
                        emit(null)
                    }
            }
        }

    init {
        observeAllLocations()
    }

    private fun observeAllLocations() {
        viewModelScope.launch {
            userDataSource.userState.first { it != null }.let { user ->

                val friendsLocationsFlow: Flow<List<LiveLocation>> = friendRepository.getFriendsByUser(user!!.id)
                    .flatMapLatest { friends ->
                        val acceptedFriendIds = friends.friends.map { it.id }
                        if (acceptedFriendIds.isNotEmpty()) {
                            locationDataSource.getFriendsLocations(acceptedFriendIds)
                        } else {
                            flowOf(emptyList())
                        }
                    }
                    .catch { e ->
                        Log.e("MapViewModel", "Error observing friend locations", e)
                        emit(emptyList())
                    }

                combine(userLocationFlow, friendsLocationsFlow) { myLocation, friendLocations ->
                    val combinedList = friendLocations.toMutableList()
                    myLocation?.let { combinedList.add(0, it) }
                    combinedList
                }.collect { finalLocationsList ->
                    _friendLocations.value = finalLocationsList
                }
            }
        }
    }
}