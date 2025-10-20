package com.example.safeher.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LiveLocation(
    val userId: String = "",
    val displayName: String = "",
    val imageUrl: String = "",
    val location: GeoPoint? = null,
    val isSharing: Boolean = false,
    @ServerTimestamp
    val lastUpdated: Date? = null,
    val sharedWith: List<String> = emptyList()
)