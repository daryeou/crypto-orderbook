package com.kwakwonjo.cryptoorderbook.core.network.api

import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitMarketResponse
import com.kwakwonjo.cryptoorderbook.core.network.model.UpbitTickerResponse
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


