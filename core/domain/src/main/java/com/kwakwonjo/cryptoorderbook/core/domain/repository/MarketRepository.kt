package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.domain.model.Market
import com.kwakwonjo.cryptoorderbook.core.domain.model.Ticker

interface MarketRepository {
    suspend fun fetchMarketList(): List<Market>

    suspend fun fetchTickerList(markets: List<String>): List<Ticker>
}


