package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import kotlinx.coroutines.flow.Flow

interface OrderBookRepository {
    fun observeOrderBook(market: String): Flow<OrderBookPayload>
}


