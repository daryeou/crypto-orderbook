package com.kwakwonjo.cryptoorderbook.core.data.mapper

import com.kwakwonjo.cryptoorderbook.core.domain.model.Market
import com.kwakwonjo.cryptoorderbook.core.domain.model.Ticker
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitMarketResponse
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