package com.sweet.app.core.network.wifi.di

import com.sweet.app.core.network.wifi.WifiNetworkMonitor
import com.sweet.app.core.network.wifi.WifiNetworkMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiModule {

    @Binds
    @Singleton
    abstract fun bindWifiNetworkMonitor(
        impl: WifiNetworkMonitorImpl
    ): WifiNetworkMonitor
}
