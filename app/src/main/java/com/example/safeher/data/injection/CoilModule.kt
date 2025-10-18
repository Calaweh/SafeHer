package com.example.safeher.data.injection

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.util.DebugLogger
import com.example.safeher.data.datasource.MediaRemoteDataSource
import com.example.safeher.data.extensions.FirestoreImageFetcher
import coil.fetch.Fetcher
import coil.request.Options
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//@Module
//@InstallIn(SingletonComponent::class)
//object CoilModule {
//    @Provides
//    @Singleton
//    fun provideImageLoader(
//        @ApplicationContext context: Context,
//        mediaRemoteDataSource: MediaRemoteDataSource
//    ): ImageLoader {
//        Log.d("CoilModule", "Creating ImageLoader")
//        val imageLoader = ImageLoader.Builder(context)
//            .components {
//                add(FirestoreImageFetcher.Factory(mediaRemoteDataSource)) // Line ~37
//                add(object : Fetcher.Factory<Any> {
//                    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
//                        Log.d("CoilDebug", "Fetcher tried with data: $data, type: ${data::class.java}")
//                        return null
//                    }
//                })
//                Log.d("CoilModule", "FirestoreImageFetcher.Factory added")
//            }
//            .logger(DebugLogger(Log.VERBOSE))
//            .build()
//        Log.d("CoilModule", "ImageLoader created: $imageLoader")
//        return imageLoader
//    }
//}