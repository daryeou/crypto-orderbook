package com.kmwh.cryptoorderbook.core.domain.repository

import com.kmwh.cryptoorderbook.core.model.MarketSummary
import kotlinx.coroutines.flow.Flow

interface MarketRepository {
    fun observeMarketSummaries(
        pollingIntervalMillis: Long,
    ): Flow<List<MarketSummary>>
}

