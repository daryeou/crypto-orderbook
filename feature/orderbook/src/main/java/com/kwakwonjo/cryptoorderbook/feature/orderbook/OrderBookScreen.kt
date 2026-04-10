package com.kwakwonjo.cryptoorderbook.feature.orderbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.LoadingContent
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.NetworkErrorOverlay
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.OrderBookContent
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.SocketErrorOverlay
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.TopBar
import com.kwakwonjo.cryptoorderbook.feature.orderbook.component.WaterOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderBookScreen(
    uiState: OrderBookContract.UiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TopBar(
                        market = uiState.marketInfo.market,
                        koreanName = uiState.marketInfo.koreanName,
                        isConnected = !uiState.isError
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val totalAsks = uiState.orderBookData?.orderBook?.asks?.sumOf { it.size } ?: 0.0
            val totalBids = uiState.orderBookData?.orderBook?.bids?.sumOf { it.size } ?: 0.0

            WaterOverlay(
                askSize = totalAsks,
                bidSize = totalBids,
                modifier = Modifier.fillMaxSize()
            )

            OrderBookContent(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = uiState.uiStatus == OrderBookContract.UiStatus.InitialLoading,
                enter = EnterTransition.None,
                exit = fadeOut(
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                )
            ) {
                LoadingContent()
            }

            if (uiState.uiStatus is OrderBookContract.Error) {
                when (uiState.uiStatus) {
                    is OrderBookContract.UiStatus.SocketError -> {
                        SocketErrorOverlay(onRetry = onRetry)
                    }
                    is OrderBookContract.UiStatus.Offline -> {
                        NetworkErrorOverlay()
                    }
                }
            }
        }
    }
}