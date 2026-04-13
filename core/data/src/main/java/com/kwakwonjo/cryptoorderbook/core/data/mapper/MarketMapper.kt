package com.kwakwonjo.cryptoorderbook.core.data.mapper

import com.kwakwonjo.cryptoorderbook.core.model.Market
import com.kwakwonjo.cryptoorderbook.core.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitMarketResponse
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerFrame
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerResponse

internal fun UpbitMarketResponse.toDomain(): Market = Market(
    market = market,
    koreanName = koreanName,
    englishName = englishName,
)

internal fun UpbitTickerResponse.toDomain(): Ticker = Ticker(
    market = market,
    tradePrice = tradePrice,
    signedChangeRate = signedChangeRate,
)

internal fun UpbitTickerFrame.toTicker(): Ticker = Ticker(
    market = market,
    tradePrice = tradePrice,
    signedChangeRate = signedChangeRate,
)
