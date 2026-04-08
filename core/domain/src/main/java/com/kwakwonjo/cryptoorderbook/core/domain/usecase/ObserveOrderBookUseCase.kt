package com.kwakwonjo.cryptoorderbook.core.domain.usecase

import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.model.OrderBookPayload
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveOrderBookUseCase @Inject constructor(
    private val orderBookRepository: OrderBookRepository,
) {
    operator fun invoke(market: String): Flow<OrderBookPayload> {
        return orderBookRepository.observeOrderBook(market)
    }
}

