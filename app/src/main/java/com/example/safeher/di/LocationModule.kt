package com.example.safeher.di

import com.example.safeher.data.utils.ILocationProvider
import com.example.safeher.data.utils.GmsLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindLocationProvider(
        gmsLocationProvider: GmsLocationProvider
    ): ILocationProvider
}