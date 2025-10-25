package com.example.safeher.di

import com.example.safeher.utils.ILocationProvider
//import com.example.safeher.utils.GmsLocationProvider
import com.example.safeher.utils.HmsLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindLocationProvider(
        locationProvider: HmsLocationProvider
    ): ILocationProvider
}