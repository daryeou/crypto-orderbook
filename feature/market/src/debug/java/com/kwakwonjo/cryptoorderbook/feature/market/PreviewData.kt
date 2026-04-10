package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker

internal object PreviewData {
    val marketItems = listOf(
        marketItem(
            market = "KRW-BTC",
            koreanName = "비트코인",
            englishName = "Bitcoin",
            tradePrice = 148_956_000.0,
            signedChangeRate = 0.0031,
        ),
        marketItem(
            market = "KRW-ETH",
            koreanName = "이더리움",
            englishName = "Ethereum",
            tradePrice = 5_486_000.0,
            signedChangeRate = -0.0124,
        ),
        marketItem(
            market = "BTC-SOL",
            koreanName = "솔라나",
            englishName = "Solana",
            tradePrice = 0.00231,
            signedChangeRate = 0.0845,
        ),
        marketItem(
            market = "USDT-XRP",
            koreanName = "리플",
            englishName = "XRP",
            tradePrice = 0.615,
            signedChangeRate = -0.0342,
        ),
    )

    private fun marketItem(
        market: String,
        koreanName: String,
        englishName: String,
        tradePrice: Double,
        signedChangeRate: Double,
    ): MarketListContract.MarketItem {
        return MarketListContract.MarketItem(
            market = Market(
                market = market,
                koreanName = koreanName,
                englishName = englishName,
            ),
            ticker = Ticker(
                market = market,
                tradePrice = tradePrice,
                signedChangeRate = signedChangeRate,
            ),
        )
    }
}
