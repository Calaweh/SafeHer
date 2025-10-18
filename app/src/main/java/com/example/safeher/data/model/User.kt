package com.example.safeher.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId val id: String = "",
    val email: String = "",
    val imageUrl: String = "",
    val displayName: String = "",
    val anonymous: Boolean = true,
    @get:ServerTimestamp val deletedAt: Date? = null
) {
    constructor() : this("", "", "", "", true, null)
}