package com.example.safeher.data.model

enum class SharingMode {
    IDLE,
    COUNTDOWN,
    SHARING
}

data class LocationSharingState(
    val mode: SharingMode = SharingMode.IDLE,
    val timeLeftInMillis: Long = 0L,
    val totalDurationInMillis: Long = 0L
)