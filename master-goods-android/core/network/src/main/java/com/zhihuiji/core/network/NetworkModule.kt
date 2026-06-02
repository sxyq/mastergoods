package com.zhihuiji.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zhihuiji.core.datastore.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private fun buildCertificatePinner(): CertificatePinner? {
        if (!BuildConfig.CERT_PINNING_ENABLED) return null
        val host = BuildConfig.PINNED_HOST.trim()
        val pins = BuildConfig.PINNED_SHA256_PINS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (host.isBlank() || pins.isEmpty()) return null
        return CertificatePinner.Builder().apply {
            pins.forEach { pin ->
                add(host, pin)
            }
        }.build()
    }

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
        level = if (BuildConfig.NETWORK_LOGGING_ENABLED) {
            HttpLoggingInterceptor.Level.HEADERS
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    @BaseUrlInterceptor
    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(settingsStore: SettingsStore): Interceptor = Interceptor { chain ->
        val currentBaseUrl = settingsStore.peekBaseUrl()
        val normalizedUrl = NetworkConfig.normalizeBaseUrl(currentBaseUrl)
        val newBaseUrl = normalizedUrl.toHttpUrl()
        check(BuildConfig.ALLOW_CLEARTEXT_BASE_URL || newBaseUrl.isHttps) {
            "Release builds require an HTTPS base URL"
        }
        check(BuildConfig.ALLOW_CLEARTEXT_BASE_URL || SettingsStore.isTrustedReleaseBaseUrl(normalizedUrl)) {
            "Release builds require a trusted production host"
        }
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
    ): OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(baseUrlInterceptor)
        addInterceptor(authInterceptor)
        addInterceptor(loggingInterceptor)
        authenticator(tokenAuthenticator)
        connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        buildCertificatePinner()?.let { certificatePinner(it) }
    }.build()

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
