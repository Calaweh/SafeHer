package com.example.safeher.data.repository

import android.content.Context
import android.content.Intent
import com.example.safeher.data.datasource.AuthRemoteDataSource
import com.example.safeher.data.service.LocationSharingService
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val locationSharingRepository: LocationSharingRepository,
    @ApplicationContext private val context: Context
) {
    val currentUser: FirebaseUser? = authRemoteDataSource.currentUser
    val currentUserIdFlow: Flow<String?> = authRemoteDataSource.currentUserIdFlow

    fun getCurrentUserId(): String {
        return currentUser?.uid ?: throw IllegalStateException("User is not logged in.")
    }

    suspend fun createGuestAccount() {
        authRemoteDataSource.createGuestAccount()
    }

    suspend fun signIn(email: String, password: String) {
        authRemoteDataSource.signIn(email, password)
    }

    suspend fun signUp(email: String, password: String) {
        authRemoteDataSource.createUser(email, password)
    }

    fun signOut() {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_STOP
        }

        context.startService(intent)
        locationSharingRepository.resetState()
        authRemoteDataSource.signOut()
    }

    suspend fun deleteAccount() {
        authRemoteDataSource.deleteAccount()
    }
}