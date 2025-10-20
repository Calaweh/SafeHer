package com.example.safeher.data.datasource

import android.util.Log
import com.example.safeher.data.model.LiveLocation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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
        locationDoc.set(locationData).await()
    }

    suspend fun stopSharingLocation(userId: String) {
        val locationDoc = firestore.collection(LOCATION_COLLECTION).document(userId)
        locationDoc.update("isSharing", false).await()
    }

    fun getFriendsLocations(friendUserIds: List<String>): Flow<List<LiveLocation>> {
        return firestore.collection(LOCATION_COLLECTION)
            .whereIn("userId", friendUserIds)
            .whereEqualTo("isSharing", true)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(LiveLocation::class.java) }
    }
}