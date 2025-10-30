package com.example.safeher.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Alert(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    constructor() : this("", "", "", 0.0, 0.0, "", null)
}