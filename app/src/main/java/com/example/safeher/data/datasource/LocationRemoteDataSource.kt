    package com.example.safeher.data.datasource

    import android.util.Log
    import com.example.safeher.data.model.LiveLocation
    import com.google.firebase.firestore.FieldValue
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.GeoPoint
    import com.google.firebase.firestore.snapshots
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.catch
    import kotlinx.coroutines.flow.combine
    import kotlinx.coroutines.flow.flatMapLatest
    import kotlinx.coroutines.flow.flowOf
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.tasks.await
    import javax.inject.Inject
    import kotlin.collections.map

    class LocationRemoteDataSource @Inject constructor(
        private val firestore: FirebaseFirestore
    ) {
        companion object {
            private const val LOCATION_COLLECTION = "live_locations"
        }

        suspend fun updateUserLocation(userId: String, displayName: String, imageUrl: String, lat: Double, lon: Double, sharedWithFriendIds: List<String>) {
            Log.d("LocationRemoteDataSource","updating user location")
            val locationDoc = firestore.collection(LOCATION_COLLECTION).document(userId)
            val locationData = mapOf(
                "userId" to userId,
                "displayName" to displayName,
                "imageUrl" to imageUrl,
                "location" to GeoPoint(lat, lon),
                "isSharing" to true,
                "lastUpdated" to FieldValue.serverTimestamp(),
                "sharedWith" to sharedWithFriendIds
            )
            try {
                locationDoc.set(locationData).await()
            } catch (e: Exception) {
                Log.e("LocationRemoteDataSource", "Failed to update location: ${e.message}", e)
            }
        }

        suspend fun stopSharingLocation(userId: String) {
            val locationDoc = firestore.collection(LOCATION_COLLECTION).document(userId)
            try {
                val snapshot = locationDoc.get().await()
                if (snapshot.exists()) {
                    locationDoc.update("isSharing", false).await()
                    Log.d("LocationRemoteDataSource", "Stopped sharing location for user: $userId")
                } else {
                    Log.w("LocationRemoteDataSource", "No location document found for user: $userId")
                }
            } catch (e: Exception) {
                Log.e("LocationRemoteDataSource", "Failed to stop sharing location: ${e.message}", e)
            }
        }

        fun getFriendsLocations(friendUserIds: List<String>): Flow<List<LiveLocation>> {
            return firestore.collection(LOCATION_COLLECTION)
                .whereIn("userId", friendUserIds)
                .whereEqualTo("isSharing", true)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(LiveLocation::class.java) }
        }

        fun observeSharedLocations(userId: String): Flow<List<LiveLocation>> {
            return firestore.collection("users")
                .document(userId)
                .collection("friends")
                .whereEqualTo("status", "accepted")
                .whereEqualTo("deletedAt", null)
                .snapshots()
                .flatMapLatest { friendsSnapshot ->
                    val friendIds = friendsSnapshot.documents.mapNotNull { it.getString("id") }

                    Log.d("LocationRemoteDataSource", "Found ${friendIds.size} accepted friends")

                    if (friendIds.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        firestore.collection(LOCATION_COLLECTION)
                            .whereIn(
                                "userId",
                                friendIds.take(10)
                            )
                            .whereEqualTo("isSharing", true)
                            .whereArrayContains("sharedWith", userId)
                            .snapshots()
                            .map { snapshot ->
                                val locations = snapshot.toObjects(LiveLocation::class.java)
                                Log.d(
                                    "LocationRemoteDataSource",
                                    "Received ${locations.size} shared locations"
                                )
                                locations
                            }
                            .catch { e ->
                                Log.e(
                                    "LocationRemoteDataSource",
                                    "Error querying shared locations",
                                    e
                                )
                                emit(emptyList())
                            }
                    }
                }
                .catch { e ->
                    Log.e("LocationRemoteDataSource", "Error observing shared locations", e)
                    emit(emptyList())
                }
        }
    }