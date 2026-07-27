package com.zhihuiji.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @Named("session")
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.sessionDataStore
    }

    @Provides
    @Singleton
    @Named("settings")
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.settingsDataStore
    }

    @Provides
    @Singleton
    @Named("sync")
    fun provideSyncDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.syncDataStore
    }
}
