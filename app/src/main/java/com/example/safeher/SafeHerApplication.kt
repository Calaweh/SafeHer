package com.example.safeher

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class SafeHerApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var imageLoaderProvider: Provider<ImageLoader>

    override fun onCreate() {
        try {
            super.onCreate()
            Coil.setImageLoader(newImageLoader())
            Log.d("SafeHerApplication", "Global ImageLoader set: ${Coil.imageLoader(this)}")
        } catch (e: Exception) {
            Log.e("SafeHerApplication", "Error in onCreate: ${e.message}", e)
            throw e
        }
    }

    override fun newImageLoader(): ImageLoader {
        try {
            val imageLoader = imageLoaderProvider.get()
            return imageLoader
        } catch (e: Exception) {
            return ImageLoader.Builder(this).build()
        }
    }
}