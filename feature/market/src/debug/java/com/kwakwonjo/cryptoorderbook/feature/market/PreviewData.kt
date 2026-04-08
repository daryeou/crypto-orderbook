package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.MarketSummary

internal object PreviewData {
    val marketSummaries = listOf(
        MarketSummary(
            market = "KRW-BTC",
            koreanName = "비트코인",
            englishName = "Bitcoin",
            tradePrice = 148_956_000.0,
            signedChangeRate = 0.0031,
        ),
        MarketSummary(
            market = "KRW-ETH",
            koreanName = "이더리움",
            englishName = "Ethereum",
            tradePrice = 5_486_000.0,
            signedChangeRate = -0.0124,
        ),
    )
}


