package com.kmwh.cryptoorderbook.core.data.repository

import com.kmwh.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kmwh.cryptoorderbook.core.model.ConnectionState
import com.kmwh.cryptoorderbook.core.model.OrderBookPayload
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OrderBookRepositoryImpl @Inject constructor() : OrderBookRepository {

    override fun observeOrderBook(market: String): Flow<OrderBookPayload> = flowOf(
        OrderBookPayload(
            connectionState = ConnectionState.Idle,
        )
    )
}
