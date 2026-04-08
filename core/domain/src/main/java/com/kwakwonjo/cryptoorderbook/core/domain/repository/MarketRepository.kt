package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary
import kotlinx.coroutines.flow.Flow

interface MarketRepository {
    fun observeMarketSummaries(
        pollingIntervalMillis: Long,
    ): Flow<List<MarketSummary>>
}


