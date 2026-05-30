package com.zhihuiji.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zhihuiji.core.datastore.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    @BaseUrlInterceptor
    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(settingsStore: SettingsStore): Interceptor = Interceptor { chain ->
        val currentBaseUrl = runBlocking { settingsStore.baseUrl.first() }
        val normalizedUrl = NetworkConfig.normalizeBaseUrl(currentBaseUrl)
        val newBaseUrl = normalizedUrl.toHttpUrl()
        val originalRequest = chain.request()
        val newUrl = originalRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()
        chain.proceed(newRequest)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        @BaseUrlInterceptor baseUrlInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.DEFAULT_FALLBACK_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideZhihuijiApi(retrofit: Retrofit): ZhihuijiApi = retrofit.create(ZhihuijiApi::class.java)
}
