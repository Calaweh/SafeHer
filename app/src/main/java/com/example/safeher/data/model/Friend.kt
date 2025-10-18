package com.example.safeher.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Friend (
    val id: String = "",
    val imageUrl: String = "",
    val displayName: String = "",
    val status: String = "",
    val deletedAt: Date? = null,
    @Transient
    val documentId: String = ""
)