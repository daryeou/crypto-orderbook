package com.kwakwonjo.cryptoorderbook.feature.market

import com.kwakwonjo.cryptoorderbook.core.model.MarketType

internal object PreviewData {
    val marketItems = listOf(
        marketItem(
            market = "KRW-BTC",
            marketType = MarketType.KRW,
            koreanName = "비트코인",
            englishName = "Bitcoin",
            tradePrice = 148_956_000.0,
            signedChangeRate = 0.0031,
        ),
        marketItem(
            market = "KRW-ETH",
            marketType = MarketType.KRW,
            koreanName = "이더리움",
            englishName = "Ethereum",
            tradePrice = 5_486_000.0,
            signedChangeRate = -0.0124,
        ),
        marketItem(
            market = "BTC-SOL",
            marketType = MarketType.BTC,
            koreanName = "솔라나",
            englishName = "Solana",
            tradePrice = 0.00231,
            signedChangeRate = 0.0845,
        ),
        marketItem(
            market = "USDT-XRP",
            marketType = MarketType.USDT,
            koreanName = "리플",
            englishName = "XRP",
            tradePrice = 0.615,
            signedChangeRate = -0.0342,
        ),
    )

    private fun marketItem(
        market: String,
        marketType: MarketType,
        koreanName: String,
        englishName: String,
        tradePrice: Double,
        signedChangeRate: Double,
    ): MarketListContract.MarketItem {
        return MarketListContract.MarketItem(
            info = MarketListContract.MarketStaticInfo(
                market = market,
                marketType = marketType,
                koreanName = koreanName,
                englishName = englishName,
            ),
            priceState = MarketListContract.MarketPrice(
                tradePrice = tradePrice,
                signedChangeRate = signedChangeRate,
            ),
        )
    }
}
