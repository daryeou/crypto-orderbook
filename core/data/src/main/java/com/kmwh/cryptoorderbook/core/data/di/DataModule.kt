package com.kmwh.cryptoorderbook.core.data.di

import com.kmwh.cryptoorderbook.core.data.repository.MarketRepositoryImpl
import com.kmwh.cryptoorderbook.core.data.repository.OrderBookRepositoryImpl
import com.kmwh.cryptoorderbook.core.domain.repository.MarketRepository
import com.kmwh.cryptoorderbook.core.domain.repository.OrderBookRepository
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
}
