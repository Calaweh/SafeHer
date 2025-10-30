package com.example.safeher.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Alert(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    constructor() : this("", "", "", null)
}