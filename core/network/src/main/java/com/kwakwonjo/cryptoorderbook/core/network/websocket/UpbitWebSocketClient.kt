package com.kwakwonjo.cryptoorderbook.core.network.websocket

import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import kotlinx.coroutines.flow.Flow

interface UpbitWebSocketClient {
    fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame>

    fun observeTickerStream(markets: List<String>): Flow<UpbitTickerFrame>
}

