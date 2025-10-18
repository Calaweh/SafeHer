package com.example.safeher.di

import com.example.safeher.data.service.ConfigurationService
import com.example.safeher.data.service.FirestoreConfigurationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindConfigurationService(
        firestoreConfigurationService: FirestoreConfigurationService
    ): ConfigurationService

}