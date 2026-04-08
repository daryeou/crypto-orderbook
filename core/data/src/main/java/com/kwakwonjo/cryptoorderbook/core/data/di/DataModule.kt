package com.kwakwonjo.cryptoorderbook.core.data.di

import com.kwakwonjo.cryptoorderbook.core.data.repository.MarketRepositoryImpl
import com.kwakwonjo.cryptoorderbook.core.data.repository.NetworkStatusRepositoryImpl
import com.kwakwonjo.cryptoorderbook.core.data.repository.OrderBookRepositoryImpl
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
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
    abstract fun bindMarketRepository(
        impl: MarketRepositoryImpl,
    ): MarketRepository

    @Binds
    @Singleton
    abstract fun bindOrderBookRepository(
        impl: OrderBookRepositoryImpl,
    ): OrderBookRepository

    @Binds
    @Singleton
    abstract fun bindNetworkStatusRepository(
        impl: NetworkStatusRepositoryImpl,
    ): NetworkStatusRepository
}

