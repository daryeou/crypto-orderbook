package com.kwakwonjo.cryptoorderbook.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kwakwonjo.cryptoorderbook.feature.market.MarketListRoute
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookNavKey
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookRoute
import com.kwakwonjo.cryptoorderbook.feature.orderbook.OrderBookViewModel

@Composable
fun CryptoOrderBookNavHost(
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? Activity
    val backStack = rememberNavBackStack(AppDestination.MarketList)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                activity?.finish()
            }
        },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.MarketList> {
                MarketListRoute(
                    onMarketClick = { market, marketLabel ->
                        backStack.add(
                            OrderBookNavKey(
                                market = market,
                                label = marketLabel,
                            )
                        )
                    }
                )
            }

            entry<OrderBookNavKey> { key ->
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

