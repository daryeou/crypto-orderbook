package com.kmwh.cryptoorderbook.core.network.websocket

import com.kmwh.cryptoorderbook.core.network.model.UpbitSubscription
import com.kmwh.cryptoorderbook.core.network.model.UpbitWsFrame
import kotlinx.coroutines.flow.Flow

interface UpbitWebSocketClient {
    fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame>
}
