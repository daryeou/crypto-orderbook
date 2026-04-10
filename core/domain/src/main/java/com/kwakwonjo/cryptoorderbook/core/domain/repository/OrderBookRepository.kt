package com.kwakwonjo.cryptoorderbook.core.domain.repository

import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import kotlinx.coroutines.flow.Flow

interface OrderBookRepository {
    fun observeOrderBook(market: String): Flow<OrderBookEvent>
}


