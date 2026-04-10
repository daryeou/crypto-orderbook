package com.kwakwonjo.cryptoorderbook.core.data.repository

import com.kwakwonjo.cryptoorderbook.core.data.mapper.toDomain
import com.kwakwonjo.cryptoorderbook.core.domain.repository.MarketRepository
import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.network.api.UpbitApi
import javax.inject.Inject

class MarketRepositoryImpl @Inject constructor(
    private val upbitApi: UpbitApi,
) : MarketRepository {

    override suspend fun fetchMarketList(): List<Market> {
        return upbitApi.getMarkets().map {
            it.toDomain()
        }
    }

    override suspend fun fetchTickerList(markets: List<String>): List<Ticker> {
        return upbitApi.getTickers(
            markets = markets.joinToString(",") { it },
        ).map {
            it.toDomain()
        }
    }
}

