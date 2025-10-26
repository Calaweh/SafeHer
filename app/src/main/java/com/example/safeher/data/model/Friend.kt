package com.example.safeher.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Friend(
    @PropertyName("id") val id: String = "",
    @PropertyName("imageUrl") val imageUrl: String = "",
    @PropertyName("displayName") val displayName: String = "",
    @PropertyName("status") val status: String = "",
    @PropertyName("direction") val direction: String = "",
    @PropertyName("deletedAt") val deletedAt: Date? = null,
    @Transient val documentId: String = "",
    @PropertyName("fromUserId") val fromUserId: String? = null,
    @PropertyName("toUserId") val toUserId: String? = null
) {
    constructor() : this("", "", "", "", "", null, "", null, null)
}