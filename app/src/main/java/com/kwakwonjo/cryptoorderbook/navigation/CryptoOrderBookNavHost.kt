package com.kwakwonjo.cryptoorderbook.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kwakwonjo.cryptoorderbook.feature.market.MarketListNavKey
import com.kwakwonjo.cryptoorderbook.feature.market.MarketListRoute
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookNavKey
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookRoute
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookViewModel

@Composable
fun CryptoOrderBookNavHost(
    modifier: Modifier = Modifier,
    onFinishRequest: () -> Unit,
) {
    val backStack = rememberNavBackStack(MarketListNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                onFinishRequest()
            }
        },
        modifier = modifier,
        entryDecorators = listOf(
            // 각 백스택 엔트리별로 저장 가능한 UI 상태를 유지
            rememberSaveableStateHolderNavEntryDecorator(),
            // 각 NavEntry에 ViewModelStoreOwner를 부여해 화면별 ViewModel Scope 분리
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<MarketListNavKey> {
                MarketListRoute(
                    onMarketClick = { market, marketType, koreanName ->
                        backStack.add(
                            OrderBookNavKey(
                                market = market,
                                marketType = marketType,
                                koreanName = koreanName,
                            )
                        )
                    }
                )
            }

            entry<OrderBookNavKey> { key ->
                // 엔트리 스코프 ViewModel을 만들고 nav key를 Hilt assisted injection으로 전달한다.
                val viewModel = hiltViewModel<OrderBookViewModel, OrderBookViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )

                OrderBookRoute(
                    onBack = { backStack.removeLastOrNull() },
                    viewModel = viewModel,
                )
            }
        },
    )
}

