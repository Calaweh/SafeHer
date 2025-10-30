package com.example.safeher.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AlertHistory(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val type: AlertType = AlertType.SENT,
    val status: AlertStatus = AlertStatus.DELIVERED,
    @ServerTimestamp
    val timestamp: Date? = null
) {
    constructor() : this("", "", "", "", "", 0.0, 0.0, "", AlertType.SENT, AlertStatus.DELIVERED, null)
}

enum class AlertType {
    SENT,
    RECEIVED
}

enum class AlertStatus {
    DELIVERED,
    ACKNOWLEDGED,
    EXPIRED
}
