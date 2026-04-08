package com.kwakwonjo.cryptoorderbook.core.domain.repository

interface NetworkStatusRepository {
    fun isNetworkAvailable(): Boolean
}
