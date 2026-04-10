package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.MarketType

internal object PreviewData {
    val marketItems = listOf(
        MarketListContract.MarketItem(
            market = "KRW-BTC",
            marketType = MarketType.KRW,
            koreanName = "비트코인",
            englishName = "Bitcoin",
            tradePrice = 148_956_000.0,
            signedChangeRate = 0.0031,
        ),
        MarketListContract.MarketItem(
            market = "KRW-ETH",
            marketType = MarketType.KRW,
            koreanName = "이더리움",
            englishName = "Ethereum",
            tradePrice = 5_486_000.0,
            signedChangeRate = -0.0124,
        ),
    )
}
