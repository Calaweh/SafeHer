package com.example.safeher.data.datasource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MEDIA_ID_PREFIX = "firestore_media:"
        private const val MAX_CHUNK_SIZE_BYTES = 900_000
        private const val MEDIA_COLLECTION = "media_metadata"
    }

    suspend fun uploadImageFromUri(uri: Uri, userId: String): String {
        return withContext(Dispatchers.IO) {
            verifyAuthentication(userId)

            val fileName = getFileName(uri) ?: "unknown_image.jpg"
            val base64String = convertImageToBase64(uri)
            val mediaId = "img_${System.currentTimeMillis()}"
            storeMedia(mediaId, base64String, fileName, "image", userId = userId)
            mediaId
        }
    }

    suspend fun uploadAudioFromUri(uri: Uri, userId: String): String {
        return withContext(Dispatchers.IO) {
            verifyAuthentication(userId)

            val fileName = getFileName(uri) ?: "unknown_audio.mp3"
            val base64String = convertAudioToBase64(uri)
            val mediaId = "aud_${System.currentTimeMillis()}"
            storeMedia(mediaId, base64String, fileName, "audio", userId = userId)
            mediaId
        }
    }

    private fun verifyAuthentication(userId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw IllegalStateException("User must be authenticated to upload media")
        }
        if (currentUser.uid != userId) {
            throw IllegalStateException("User ID mismatch: cannot upload media for different user")
        }
    }

    private suspend fun storeMedia(mediaId: String, base64String: String, fileName: String, type: String, userId: String) {

        Log.d("FirestoreMediaRepo", "Attempting to store media. mediaId: $mediaId, userId: $userId")
        val metadataRef = firestore.collection(MEDIA_COLLECTION).document(mediaId)
        try {
            val chunks = base64String.chunked(MAX_CHUNK_SIZE_BYTES)
            val totalChunks = chunks.size

            val metadata = hashMapOf(
                "userId" to userId,
                "type" to type,
                "fileName" to fileName,
                "totalChunks" to totalChunks.toLong(),
                "originalSize" to base64String.length.toLong(),
                "timestamp" to System.currentTimeMillis(),
                "status" to "uploading"
            )

            metadataRef.set(metadata).await()
            Log.d("FirestoreMediaRepo", "Metadata document created successfully")

            chunks.forEachIndexed { index, chunk ->
                val chunkRef = metadataRef.collection("chunks").document(index.toString())
                val chunkData = hashMapOf("chunk" to chunk)
                chunkRef.set(chunkData).await()
                Log.d("FirestoreMediaRepo", "Chunk $index uploaded successfully")
            }

            metadataRef.update("status", "completed").await()
            Log.d("FirestoreMediaRepo", "$type '$fileName' stored successfully as $mediaId")

        } catch (e: Exception) {
            Log.e("FirestoreMediaRepo", "Error storing $type: ${e.message}", e)
            try {
                metadataRef.update("status", "failed", "error", e.message).await()
            } catch (updateException: Exception) {
                Log.e("FirestoreMediaRepo", "Failed to update error status", updateException)
            }
            throw IllegalStateException("Failed to upload media to Firestore: ${e.message}", e)
        }
    }

    private fun convertImageToBase64(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image file from URI.")
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Cannot decode image from stream.")
        inputStream.close()

        val outputStream = ByteArrayOutputStream()
        var quality = 90
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > 5_000_000 && quality > 20)

        Log.d("MediaRemoteDataSource", "Final compressed image size: ${outputStream.size()} bytes")

        val byteArray = outputStream.toByteArray()
        bitmap.recycle()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun convertAudioToBase64(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open audio file from URI.")
        val bytes = inputStream.readBytes()
        inputStream.close()
        // Add a size check if necessary, e.g., 50MB limit
        if (bytes.size > 50_000_000) {
            throw Exception("Audio file is too large (max 50MB).")
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return null
    }

    suspend fun getMediaData(mediaId: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirestoreMediaRepo", "Retrieving media data for ID: $mediaId")
                val metadataRef = firestore.collection(MEDIA_COLLECTION).document(mediaId)
                val metadataDoc = metadataRef.get().await()

                if (!metadataDoc.exists()) {
                    throw IllegalStateException("Media metadata not found for ID: $mediaId")
                }

                val status = metadataDoc.getString("status")
                if (status != "completed") {
                    throw IllegalStateException("Media is not ready. Status: $status")
                }

                val totalChunks = metadataDoc.getLong("totalChunks")?.toInt()
                    ?: throw IllegalStateException("Invalid metadata: totalChunks missing.")

                val chunkQuery = metadataRef.collection("chunks")
                    .orderBy(com.google.firebase.firestore.FieldPath.documentId())
                    .get()
                    .await()

                if (chunkQuery.documents.size != totalChunks) {
                    throw IllegalStateException("Incomplete media data: Expected $totalChunks chunks, but found ${chunkQuery.documents.size}")
                }

                val fullBase64String = chunkQuery.documents.joinToString("") { doc ->
                    doc.getString("chunk") ?: throw IllegalStateException("Chunk data missing in doc ${doc.id}")
                }

                Log.d("FirestoreMediaRepo", "Successfully reassembled ${chunkQuery.documents.size} chunks.")

                Base64.decode(fullBase64String, Base64.NO_WRAP)

            } catch (e: Exception) {
                Log.e("FirestoreMediaRepo", "Failed to retrieve media data for ID: $mediaId", e)
                throw e
            }
        }
    }
}