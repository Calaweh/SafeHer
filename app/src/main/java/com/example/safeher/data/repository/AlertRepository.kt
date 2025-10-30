package com.example.safeher.data.repository

import android.util.Log
import com.example.safeher.data.model.Alert
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "AlertRepository"

    fun getNewAlerts(userId: String): Flow<List<Alert>> = callbackFlow {
        val collectionPath = "alerts/$userId/pending_alerts"
        val collectionRef = firestore.collection(collectionPath)

        val listenerRegistration: ListenerRegistration =
            collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for alerts", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { it.toObject<Alert>() }
                    trySend(alerts)
                    Log.d(TAG, "Received ${alerts.size} new alerts for user $userId")
                }
            }

        awaitClose {
            Log.d(TAG, "Closing alerts listener for user $userId")
            listenerRegistration.remove()
        }
    }

    suspend fun deleteAlert(userId: String, alertId: String) {
        try {
            firestore.collection("alerts")
                .document(userId)
                .collection("pending_alerts")
                .document(alertId)
                .delete()
                .await()
            Log.d(TAG, "Successfully deleted alert: $alertId for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting alert: $alertId", e)
        }
    }
}