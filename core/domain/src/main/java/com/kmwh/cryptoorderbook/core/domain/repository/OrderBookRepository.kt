package com.kmwh.cryptoorderbook.core.domain.repository

import com.kmwh.cryptoorderbook.core.model.OrderBookPayload
import kotlinx.coroutines.flow.Flow

interface OrderBookRepository {
    fun observeOrderBook(market: String): Flow<OrderBookPayload>
}

