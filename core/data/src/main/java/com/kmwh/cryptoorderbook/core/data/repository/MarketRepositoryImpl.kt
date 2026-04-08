package com.kmwh.cryptoorderbook.core.data.repository

import com.kmwh.cryptoorderbook.core.domain.repository.MarketRepository
import com.kmwh.cryptoorderbook.core.model.MarketSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MarketRepositoryImpl @Inject constructor() : MarketRepository {

    override fun observeMarketSummaries(
        pollingIntervalMillis: Long,
    ): Flow<List<MarketSummary>> = flowOf(emptyList())
}
