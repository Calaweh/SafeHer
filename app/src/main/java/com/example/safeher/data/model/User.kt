package com.example.safeher.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class User(
    @DocumentId val id: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("imageUrl") val imageUrl: String = "",
    @PropertyName("displayName") val displayName: String = "",
    @PropertyName("anonymous") val anonymous: Boolean = true,
    @PropertyName("deletedAt") val deletedAt: Date? = null
) {
    constructor() : this("", "", "", "", true, null)
}