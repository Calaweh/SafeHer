package com.example.safeher.di

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.request.Options
import coil.util.DebugLogger
import com.example.safeher.BuildConfig
import com.example.safeher.data.datasource.MediaRemoteDataSource
import com.example.safeher.data.extensions.FirestoreImageFetcher
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

//    @Provides
//    @Singleton
//    fun provideImageLoader(
//        app: Application,
//        okHttpClient: OkHttpClient
//    ): ImageLoader {
//        return ImageLoader.Builder(app)
//            .okHttpClient(okHttpClient)
//            .crossfade(true)
//            .respectCacheHeaders(false)
//            .build()
//    }

    @Provides
    @Singleton
    fun provideImageLoader(
        app: Application,
        okHttpClient: OkHttpClient,
        mediaRemoteDataSource: MediaRemoteDataSource
    ): ImageLoader {
        Log.d("AppModule", "Creating custom ImageLoader with FirestoreFetcher")

        return ImageLoader.Builder(app)
            .okHttpClient(okHttpClient)
            .components {
                add(FirestoreImageFetcher.Factory(mediaRemoteDataSource))
                add(object : Fetcher.Factory<Any> {
                    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
                        Log.v("AppModule", "Fetcher tried for data: $data")
                        return null
                    }
                })
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .logger(DebugLogger(Log.VERBOSE))
            .build()
    }
}