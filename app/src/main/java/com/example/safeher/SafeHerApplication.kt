package com.example.safeher

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.huawei.agconnect.config.AGConnectServicesConfig
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

            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBTc8kXha7RFq6-yW_VaIWKB7dLosNFkgA")
                .setApplicationId("1:616469505955:android:d486bf511d0b0ed01cdc16")
                .setProjectId("safeher-a2f47")
                .setStorageBucket("safeher-a2f47.firebasestorage.app")
                .build()

            FirebaseApp.initializeApp(this, options)
            Log.d("SafeHerApplication", "Firebase initialized successfully")

            AGConnectServicesConfig.fromContext(this)
            Log.d("SafeHerApplication", "AGConnect initialized successfully")

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