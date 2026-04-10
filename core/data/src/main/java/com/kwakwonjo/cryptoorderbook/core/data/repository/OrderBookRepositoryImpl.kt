package com.kwakwonjo.cryptoorderbook.core.data.repository

import com.kwakwonjo.cryptoorderbook.core.data.mapper.toDomain
import com.kwakwonjo.cryptoorderbook.core.domain.model.OrderBookEvent
import com.kwakwonjo.cryptoorderbook.core.domain.repository.OrderBookRepository
import com.kwakwonjo.cryptoorderbook.core.model.ConnectionState
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import com.kwakwonjo.cryptoorderbook.core.network.websocket.UpbitWebSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.runningFold
import java.io.IOException
import javax.inject.Inject
import kotlin.compareTo

class OrderBookRepositoryImpl @Inject constructor(
    private val upbitWebSocketClient: UpbitWebSocketClient,
) : OrderBookRepository {

    override fun observeOrderBook(market: String, orderbookUnit: Int): Flow<OrderBookEvent> {
        val initialPayload = OrderBookEvent(connectionState = ConnectionState.Connecting)

        return upbitWebSocketClient.observeUpbitStream(
            subscription = UpbitSubscription(market = market, orderbookUnit = orderbookUnit),
        ).retryWhen { cause, attempt ->
            if (cause is IOException && attempt < 3) {
                val delayTime = minOf(1000L * (attempt + 1), 10000L)
                delay(delayTime)
                true
            } else {
                false
            }
        }.runningFold(initialPayload) { current, frame ->
            current.reduce(frame)
        }.catch { throwable ->
            emit(
                OrderBookEvent(
                    connectionState = ConnectionState.Error,
                    errorMessage = throwable.message,
                )
            )
        }
    }

    private fun OrderBookEvent.reduce(frame: UpbitWsFrame): OrderBookEvent {
        return when (frame) {
            is UpbitOrderBookFrame -> copy(
                connectionState = ConnectionState.Connected,
                orderBook = frame.toDomain(),
                errorMessage = null,
            )

            is UpbitTickerFrame -> copy(
                connectionState = ConnectionState.Connected,
                ticker = frame.toDomain(),
                errorMessage = null,
            )
        }
    }
}

