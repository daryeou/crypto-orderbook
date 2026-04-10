package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kwakwonjo.cryptoorderbook.core.model.MarketType

sealed interface MarketListContract {

    @Immutable
    data class MarketItem(
        val info: MarketStaticInfo,
        val priceState: MarketPrice,
    )

    @Immutable
    data class MarketStaticInfo(
        val market: String,
        val marketType: MarketType,
        val koreanName: String,
        val englishName: String,
    ) {
        val symbol: String by lazy {
            market.split("-").getOrElse(1) { market }
        }

        val icon: String by lazy {
            "https://cdn.jsdelivr.net/gh/prasangapokharel/crypto-icons@v1.0.0/binance/${symbol.uppercase()}.png"
        }
    }

    @Immutable
    data class MarketPrice(
        val tradePrice: Double,
        val signedChangeRate: Double,
    )

    enum class UiStatus {
        IDLE,
        INITIAL_LOADING,
        ERROR
    }

    @Stable
    data class UiState(
        val items: List<MarketItem>,
        val uiStatus: UiStatus
    )
}
