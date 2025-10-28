package com.example.safeher.di

import android.content.Context
import android.util.Log
import com.example.safeher.utils.FallbackLocationProvider
import com.example.safeher.utils.HmsLocationProvider
import com.example.safeher.utils.ILocationProvider
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): ILocationProvider {
        val hmsResult = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(context)
        val hmsAvailable = hmsResult == ConnectionResult.SUCCESS

        Log.d("LocationModule", "HMS Available: $hmsAvailable (code: $hmsResult)")

        return if (hmsAvailable) {
            try {
                Log.d("LocationModule", "Using HMS Location Provider")
                HmsLocationProvider(context)
            } catch (e: Exception) {
                Log.e("LocationModule", "HMS provider failed, using fallback", e)
                FallbackLocationProvider(context)
            }
        } else {
            Log.d("LocationModule", "Using Fallback Location Provider")
            FallbackLocationProvider(context)
        }
    }
}