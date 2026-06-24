package com.zhihuiji.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zhihuiji.core.datastore.SessionStore
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
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val fallbackBaseUrl = NetworkConfig.DEFAULT_FALLBACK_URL.toHttpUrl()

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
        classDiscriminator = "event_type"
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
        val newBaseUrl = currentBaseUrl.toHttpUrl()
        check(BuildConfig.ALLOW_CLEARTEXT_BASE_URL || newBaseUrl.isHttps) {
            "Release builds require an HTTPS base URL"
        }
        check(BuildConfig.ALLOW_CLEARTEXT_BASE_URL || SettingsStore.isTrustedReleaseBaseUrl(currentBaseUrl)) {
            "Release builds require a trusted production host"
        }
        val originalRequest = chain.request()
        val newUrl = rewriteUrlForBaseUrl(
            originalUrl = originalRequest.url,
            newBaseUrl = newBaseUrl,
        )
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()
        chain.proceed(newRequest)
    }

    internal fun rewriteUrlForBaseUrl(
        originalUrl: okhttp3.HttpUrl,
        newBaseUrl: okhttp3.HttpUrl,
        fallbackUrl: okhttp3.HttpUrl = fallbackBaseUrl,
    ): okhttp3.HttpUrl {
        val fallbackPathPrefix = fallbackUrl.encodedPath.removeSuffix("/")
        val relativePath = when {
            fallbackPathPrefix.isNotEmpty() && originalUrl.encodedPath.startsWith("$fallbackPathPrefix/") ->
                originalUrl.encodedPath.removePrefix("$fallbackPathPrefix/")
            originalUrl.encodedPath == fallbackPathPrefix -> ""
            else -> originalUrl.encodedPath.removePrefix("/")
        }
        val rewrittenPath = when {
            relativePath.isBlank() -> newBaseUrl.encodedPath
            newBaseUrl.encodedPath.endsWith("/") -> newBaseUrl.encodedPath + relativePath
            else -> "${newBaseUrl.encodedPath}/$relativePath"
        }
        return originalUrl.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .encodedPath(rewrittenPath)
            .build()
    }

    @Provides
    @Singleton
    fun provideCacheScopeProvider(
        settingsStore: SettingsStore,
        sessionStore: SessionStore,
    ): CacheScopeProvider = object : CacheScopeProvider {
        override fun scopeKey(): String {
            val userId = sessionStore.peekUserId()?.toString() ?: "anonymous"
            return "base=${settingsStore.peekBaseUrl()}|user=$userId"
        }
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
        connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
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

    @Provides
    @Singleton
    fun provideZhihuijiV2Api(retrofit: Retrofit): ZhihuijiV2Api = retrofit.create(ZhihuijiV2Api::class.java)

    @Provides
    @Singleton
    fun provideAgentSseClient(
        okHttpClient: OkHttpClient,
        json: Json,
        settingsStore: SettingsStore,
    ): AgentSseClient = AgentSseClient(
        okHttpClient = okHttpClient,
        json = json,
        baseUrlProvider = { settingsStore.peekBaseUrl() },
    )
}
