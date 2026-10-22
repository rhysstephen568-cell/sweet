package com.sweet.app.core.network.nearby.di

import com.sweet.app.core.network.nearby.NsdPeerDiscoveryRepositoryImpl
import com.sweet.app.core.network.nearby.PeerDiscoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NearbyModule {

    @Binds
    @Singleton
    abstract fun bindPeerDiscoveryRepository(
        impl: NsdPeerDiscoveryRepositoryImpl
    ): PeerDiscoveryRepository
}
