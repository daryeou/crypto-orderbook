package com.kwakwonjo.cryptoorderbook.core.network.di

import com.kwakwonjo.cryptoorderbook.core.network.api.UpbitApi
import com.kwakwonjo.cryptoorderbook.core.network.websocket.OkHttpUpbitWebSocketClient
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.upbit.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    fun provideUpbitApi(retrofit: Retrofit): UpbitApi = retrofit.create(UpbitApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketModule {
    @Binds
    @Singleton
    abstract fun bindUpbitWebSocketClient(
        impl: OkHttpUpbitWebSocketClient,
    ): UpbitWebSocketClient
}

