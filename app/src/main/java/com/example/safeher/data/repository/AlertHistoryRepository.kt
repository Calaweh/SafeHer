package com.example.safeher.data.repository

import android.util.Log
import com.example.safeher.data.model.AlertHistory
import com.example.safeher.data.model.AlertStatus
import com.example.safeher.data.model.AlertType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertHistoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "AlertHistoryRepository"

    suspend fun saveAlertHistory(alertHistory: AlertHistory) {
        try {
            firestore.collection("alert_history")
                .add(alertHistory)
                .await()
            Log.d(TAG, "Alert history saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alert history", e)
        }
    }

    fun getAlertHistory(userId: String): Flow<List<AlertHistory>> = callbackFlow {
        var sentAlerts: List<AlertHistory> = emptyList()
        var receivedAlerts: List<AlertHistory> = emptyList()

        fun emitCombined() {
            val allAlerts = (sentAlerts + receivedAlerts).sortedByDescending { it.timestamp }
            trySend(allAlerts)
            Log.d(TAG, "Loaded ${allAlerts.size} alerts for user $userId (${sentAlerts.size} sent, ${receivedAlerts.size} received)")
        }

        val sentListener = firestore.collection("alert_history")
            .whereEqualTo("senderId", userId)
            .whereEqualTo("type", AlertType.SENT.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for sent alerts", error)
                    return@addSnapshotListener
                }

                sentAlerts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AlertHistory::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d(TAG, "User $userId sent alerts updated: ${sentAlerts.size}")
                emitCombined()
            }

        val receivedListener = firestore.collection("alert_history")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("type", AlertType.RECEIVED.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for received alerts", error)
                    return@addSnapshotListener
                }

                receivedAlerts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AlertHistory::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d(TAG, "User $userId received alerts updated: ${receivedAlerts.size}")
                emitCombined()
            }

        awaitClose {
            Log.d(TAG, "Closing alert history listeners")
            sentListener.remove()
            receivedListener.remove()
        }
    }

    fun getSentAlerts(userId: String): Flow<List<AlertHistory>> = callbackFlow {
        val listenerRegistration = firestore.collection("alert_history")
            .whereEqualTo("senderId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for sent alerts", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AlertHistory::class.java)?.copy(id = doc.id)
                    }
                    trySend(alerts)
                    Log.d(TAG, "Loaded ${alerts.size} sent alerts")
                }
            }

        awaitClose {
            Log.d(TAG, "Closing sent alerts listener")
            listenerRegistration.remove()
        }
    }

    fun getReceivedAlerts(userId: String): Flow<List<AlertHistory>> = callbackFlow {
        val listenerRegistration = firestore.collection("alert_history")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for received alerts", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AlertHistory::class.java)?.copy(id = doc.id)
                    }
                    trySend(alerts)
                    Log.d(TAG, "Loaded ${alerts.size} received alerts")
                }
            }

        awaitClose {
            Log.d(TAG, "Closing received alerts listener")
            listenerRegistration.remove()
        }
    }

    suspend fun updateAlertStatus(alertId: String, status: AlertStatus) {
        try {
            firestore.collection("alert_history")
                .document(alertId)
                .update("status", status.name)
                .await()
            Log.d(TAG, "Alert status updated to $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating alert status", e)
        }
    }

    suspend fun deleteOldAlerts(daysOld: Int = 30) {
        try {
            val cutoffDate = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)

            firestore.collection("alert_history")
                .whereLessThan("timestamp", cutoffDate)
                .get()
                .await()
                .documents
                .forEach { it.reference.delete() }

            Log.d(TAG, "Deleted alerts older than $daysOld days")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting old alerts", e)
        }
    }
}