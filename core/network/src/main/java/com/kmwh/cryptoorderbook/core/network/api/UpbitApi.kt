package com.kmwh.cryptoorderbook.core.network.api

import com.kmwh.cryptoorderbook.core.network.model.UpbitMarketResponse
import com.kmwh.cryptoorderbook.core.network.model.UpbitTickerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UpbitApi {
    @GET("v1/market/all")
    suspend fun getMarkets(
        @Query("isDetails") isDetails: Boolean = false,
    ): List<UpbitMarketResponse>

    @GET("v1/ticker")
    suspend fun getTickers(
        @Query("markets") markets: String,
    ): List<UpbitTickerResponse>
}

