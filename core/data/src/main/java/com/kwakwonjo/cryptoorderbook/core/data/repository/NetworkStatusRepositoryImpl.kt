package com.kwakwonjo.cryptoorderbook.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.kwakwonjo.cryptoorderbook.core.domain.repository.NetworkStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NetworkStatusRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkStatusRepository {

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        // NET_CAPABILITY_INTERNET: 이 네트워크가 인터넷 연결 용도로 구성되어 있는지 확인
        // NET_CAPABILITY_VALIDATED: 실제로 외부 인터넷 연결이 검증된 상태인지 확인
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
