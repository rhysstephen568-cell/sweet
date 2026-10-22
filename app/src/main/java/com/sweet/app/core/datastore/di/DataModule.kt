package com.sweet.app.core.datastore.di

import com.sweet.app.core.datastore.UserPreferencesRepository
import com.sweet.app.core.datastore.UserPreferencesRepositoryImpl
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
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}
