package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketListScreen(
    uiState: MarketListContract.UiState,
    onMarketClick: (market: String, marketType: MarketType, koreanName: String) -> Unit,
    onRetry: () -> Unit,
) {
    val marketTypes = MarketType.entries
    val pagerState = rememberPagerState(pageCount = { marketTypes.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.market_list_title))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MarketTypeTabs(
                selectedMarketType = marketTypes[pagerState.currentPage],
                onMarketTypeSelected = { marketType ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(marketTypes.indexOf(marketType))
                    }
                },
            )

            when (uiState.uiStatus) {
                MarketListContract.UiStatus.INITIAL_LOADING -> {
                    if (uiState.items.isEmpty()) {
                        LoadingContent(modifier = Modifier.weight(1f))
                    } else {
                        MarketListContent(
                            marketItems = uiState.items,
                            onMarketClick = onMarketClick,
                            pagerState = pagerState,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                MarketListContract.UiStatus.ERROR -> {
                    if (uiState.items.isEmpty()) {
                        ErrorContent(
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        MarketListContent(
                            marketItems = uiState.items,
                            onMarketClick = onMarketClick,
                            pagerState = pagerState,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                MarketListContract.UiStatus.IDLE -> MarketListContent(
                    marketItems = uiState.items,
                    onMarketClick = onMarketClick,
                    pagerState = pagerState,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MarketTypeTabs(
    selectedMarketType: MarketType,
    onMarketTypeSelected: (MarketType) -> Unit,
) {
    val markets = MarketType.entries

    PrimaryTabRow(selectedTabIndex = markets.indexOf(selectedMarketType)) {
        markets.forEach { marketType ->
            Tab(
                selected = selectedMarketType == marketType,
                onClick = { onMarketTypeSelected(marketType) },
                text = { Text(text = marketType.name) },
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.market_list_loading),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.market_list_error),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = stringResource(R.string.market_list_retry))
        }
    }
}

@Composable
private fun MarketListContent(
    marketItems: List<MarketListContract.MarketItem>,
    onMarketClick: (market: String, marketType: MarketType, koreanName: String) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
    ) { page ->
        val pageMarketType = MarketType.entries[page]
        val filteredMarkets = marketItems.filter { it.marketType == pageMarketType }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = filteredMarkets,
                key = { it.market },
            ) { market ->
                MarketRow(
                    market = market,
                    onClick = {
                        onMarketClick(
                            market.market,
                            market.marketType,
                            market.koreanName,
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MarketRow(
    market: MarketListContract.MarketItem,
    onClick: () -> Unit,
) {
    val changeColor = if (market.signedChangeRate >= 0) {
        Color(0xFFD32F2F)
    } else {
        Color(0xFF1976D2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = market.koreanName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.market_list_row_subtitle,
                    market.englishName,
                    market.market,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = market.tradePrice.toPriceString(market.marketType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = market.signedChangeRate.toPercentString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = changeColor,
                )
            }
        }
    }
}

private fun Double.toPriceString(marketType: MarketType): String = when (marketType) {
    MarketType.KRW -> NumberFormat.getNumberInstance(Locale.KOREA).format(this)
    else -> DECIMAL_PRICE_FORMAT.format(this)
}

private fun Double.toPercentString(): String = String.format(Locale.KOREA, "%+.2f%%", this * 100)

private val DECIMAL_PRICE_FORMAT = DecimalFormat("#,##0.#########")
