package com.example.safeher.data.extensions

import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.safeher.data.datasource.MediaRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

class FirestoreImageFetcher(
    private val mediaId: String,
    private val options: Options,
    private val mediaRemoteDataSource: MediaRemoteDataSource
) : Fetcher {
    override suspend fun fetch(): FetchResult? {

        Log.i("FirestoreImageFetcher", "FETCHING data for mediaId: $mediaId")

        return try {
            val imageData: ByteArray = withContext(Dispatchers.IO) {
                mediaRemoteDataSource.getMediaData(mediaId)
            }
            Log.i("FirestoreImageFetcher", "SUCCESS! Fetched ${imageData.size} bytes for $mediaId")
            SourceResult(
                source = ImageSource(Buffer().write(imageData), options.context),
                mimeType = "image/jpeg",
                dataSource = DataSource.NETWORK
            )
        } catch (e: Exception) {
            Log.e("FirestoreImageFetcher", "FAILURE fetching data for $mediaId", e)
            null
        }
    }

    class Factory(
        private val mediaRemoteDataSource: MediaRemoteDataSource
    ) : Fetcher.Factory<Uri> {
        init {
            Log.d("FirestoreImageFetcher", "Factory initialized with MediaRemoteDataSource: $mediaRemoteDataSource")
        }

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            Log.d("FirestoreImageFetcher", "Factory.create() CALLED")
            Log.d("FirestoreImageFetcher", "Input data: '$data'")
            Log.d("FirestoreImageFetcher", "Data type: ${data::class.java.simpleName}")
            Log.d("FirestoreImageFetcher", "Expected prefix: '${MediaRemoteDataSource.MEDIA_ID_PREFIX}'")
            val dataString = data.toString()
            Log.d("FirestoreImageFetcher", "Data as string: '$dataString'")
            Log.d("FirestoreImageFetcher", "Prefix match: ${dataString.startsWith(MediaRemoteDataSource.MEDIA_ID_PREFIX)}")
            if (!dataString.startsWith(MediaRemoteDataSource.MEDIA_ID_PREFIX)) {
                Log.d("FirestoreImageFetcher", "No match for prefix, returning null")
                return null
            }
            val mediaId = dataString.removePrefix(MediaRemoteDataSource.MEDIA_ID_PREFIX)
            Log.i("FirestoreImageFetcher", "MATCH! Creating fetcher for mediaId: '$mediaId'")

            return FirestoreImageFetcher(mediaId, options, mediaRemoteDataSource)
        }
    }
}