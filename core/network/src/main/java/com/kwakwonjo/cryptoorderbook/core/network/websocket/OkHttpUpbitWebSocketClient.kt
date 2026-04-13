package com.kwakwonjo.cryptoorderbook.core.network.websocket

import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookMessage
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitOrderBookUnitFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitSubscription
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerMessage
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitWsFrame
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

@Singleton
class OkHttpUpbitWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : UpbitWebSocketClient {

    override fun observeUpbitStream(subscription: UpbitSubscription): Flow<UpbitWsFrame> = observeSocket(
        requestPayload = buildOrderBookPayload(subscription),
        parseMessage = ::parseCompositeMessage,
    )

    override fun observeTickerStream(markets: List<String>): Flow<UpbitTickerFrame> = observeSocket(
        requestPayload = buildTickerPayload(markets),
        parseMessage = ::parseTickerMessage,
    )

    private fun <T> observeSocket(
        requestPayload: String,
        parseMessage: (String) -> T?,
    ): Flow<T> = callbackFlow {
        val request = Request.Builder()
            .url(UPBIT_WEBSOCKET_URL)
            .build()

        val webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(requestPayload)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseMessage(text)?.let { trySend(it) }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    parseMessage(bytes.utf8())?.let { trySend(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    close(t)
                }
            }
        )

        awaitClose {
            webSocket.close(1000, null)
        }
    }

    private fun buildOrderBookPayload(subscription: UpbitSubscription): String {
        val payload = buildJsonArray {
            add(
                buildJsonObject {
                    put("ticket", UUID.randomUUID().toString())
                }
            )
            add(
                buildJsonObject {
                    put("type", "orderbook")
                    putJsonArray("codes") {
                        add(JsonPrimitive("${subscription.market}.${subscription.orderbookUnit}"))
                    }
                }
            )
            add(
                buildJsonObject {
                    put("type", "ticker")
                    putJsonArray("codes") {
                        add(JsonPrimitive(subscription.market))
                    }
                }
            )
            add(
                buildJsonObject {
                    put("format", "DEFAULT")
                }
            )
        }

        return payload.toString()
    }

    private fun buildTickerPayload(markets: List<String>): String {
        val payload = buildJsonArray {
            add(
                buildJsonObject {
                    put("ticket", UUID.randomUUID().toString())
                }
            )
            add(
                buildJsonObject {
                    put("type", "ticker")
                    putJsonArray("codes") {
                        markets.forEach { market ->
                            add(JsonPrimitive(market))
                        }
                    }
                    put("is_only_realtime", true)
                }
            )
            add(
                buildJsonObject {
                    put("format", "DEFAULT")
                }
            )
        }

        return payload.toString()
    }

    private fun parseCompositeMessage(rawMessage: String): UpbitWsFrame? {
        val jsonElement = json.parseToJsonElement(rawMessage)
        val messageType = jsonElement.jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: return null

        return when (messageType) {
            "orderbook" -> {
                val message = json.decodeFromJsonElement<UpbitOrderBookMessage>(jsonElement)
                UpbitOrderBookFrame(
                    market = message.code,
                    totalAskSize = message.totalAskSize,
                    totalBidSize = message.totalBidSize,
                    units = message.orderbookUnits.map { unit ->
                        UpbitOrderBookUnitFrame(
                            askPrice = unit.askPrice,
                            bidPrice = unit.bidPrice,
                            askSize = unit.askSize,
                            bidSize = unit.bidSize,
                        )
                    },
                    timestamp = message.timestamp,
                )
            }

            "ticker" -> parseTickerMessage(rawMessage)

            else -> null
        }
    }

    private fun parseTickerMessage(rawMessage: String): UpbitTickerFrame? {
        val message = json.decodeFromString<UpbitTickerMessage>(rawMessage)
        return UpbitTickerFrame(
            market = message.code,
            tradePrice = message.tradePrice,
            signedChangeRate = message.signedChangeRate,
            timestamp = message.timestamp,
        )
    }

    private companion object {
        const val UPBIT_WEBSOCKET_URL = "wss://api.upbit.com/websocket/v1"
    }
}

