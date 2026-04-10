package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.VerticalSpacer
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.feature.market.component.ErrorContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.LoadingContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.MarketListContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.MarketSearchBar
import com.kwakwonjo.cryptoorderbook.feature.market.component.MarketTypeTabs
import com.kwakwonjo.cryptoorderbook.feature.market.component.SortOrder
import com.kwakwonjo.cryptoorderbook.feature.market.component.TopBar
import kotlinx.coroutines.launch

@Composable
fun MarketListScreen(
    uiState: MarketListContract.UiState,
    onMarketClick: (market: String, marketType: MarketType, koreanName: String) -> Unit,
    onRetry: () -> Unit,
) {
    val marketTypes = remember { MarketType.entries.filter { it.isVisible } }
    val pagerState = rememberPagerState(pageCount = { marketTypes.size })
    val coroutineScope = rememberCoroutineScope()
    var currentSortOrder by remember { mutableStateOf(SortOrder.PRICE_DESC) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(
                searchQuery = searchQuery,
                currentSortOrder = currentSortOrder,
                onChangeSearchQuery = { query -> searchQuery = query },
                onChangeSortOrder = { sortOrder -> currentSortOrder = sortOrder },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MarketTypeTabs(
                marketTypes = marketTypes,
                selectedMarketType = marketTypes[pagerState.currentPage],
                onMarketTypeSelected = { marketType ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(marketTypes.indexOf(marketType))
                    }
                },
            )

            VerticalSpacer(8.dp)

            when (uiState.uiStatus) {
                MarketListContract.UiStatus.INITIAL_LOADING -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }

                MarketListContract.UiStatus.ERROR -> {
                    ErrorContent(
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                }

                MarketListContract.UiStatus.IDLE -> {
                    MarketListContent(
                        modifier = Modifier.weight(1f),
                        marketItems = uiState.items,
                        sortOrder = currentSortOrder,
                        searchQuery = searchQuery,
                        onMarketClick = onMarketClick,
                        pagerState = pagerState,
                        marketTypes = marketTypes,
                    )
                }
            }
        }
    }
}
