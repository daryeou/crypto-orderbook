package com.kwakwonjo.cryptoorderbook.feature.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.VerticalSpacer
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.feature.market.component.ErrorContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.LoadingContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.MarketListContent
import com.kwakwonjo.cryptoorderbook.feature.market.component.MarketTypeTabs
import com.kwakwonjo.cryptoorderbook.feature.market.component.SortOrder
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
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

    // 정렬 상태 관리
    var currentSortOrder by remember { mutableStateOf(SortOrder.PRICE_DESC) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_market_list),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                HorizontalSpacer(12.dp)
                // 검색 바 Placeholder
                SearchBarPlaceholder(modifier = Modifier.weight(1f))

                Box {
                    IconButton(onClick = { isSortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(order.labelRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentSortOrder == order) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    currentSortOrder = order
                                    isSortMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (currentSortOrder == order) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
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

                MarketListContract.UiStatus.IDLE -> MarketListContent(
                    modifier = Modifier.weight(1f),
                    marketItems = uiState.items,
                    sortOrder = currentSortOrder,
                    onMarketClick = onMarketClick,
                    pagerState = pagerState,
                    marketTypes = marketTypes,
                )
            }
        }
    }
}

@Composable
private fun SearchBarPlaceholder(modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalSpacer(8.dp)
            Text(
                text = "검색",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
