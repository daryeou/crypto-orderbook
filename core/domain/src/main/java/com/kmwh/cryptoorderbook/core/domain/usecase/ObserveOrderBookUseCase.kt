package com.kmwh.cryptoorderbook.core.domain.usecase

import com.kmwh.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kmwh.cryptoorderbook.core.model.OrderBookPayload
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveOrderBookUseCase @Inject constructor(
    private val orderBookRepository: OrderBookRepository,
) {
    operator fun invoke(market: String): Flow<OrderBookPayload> {
        return orderBookRepository.observeOrderBook(market)
    }
}
