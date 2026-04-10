package com.kwakwonjo.cryptoorderbook.feature.market.component

import androidx.annotation.StringRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.LocalColors
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.core.ui.component.MarketIcon
import com.kwakwonjo.cryptoorderbook.feature.market.MarketListContract
import com.kwakwonjo.cryptoorderbook.feature.market.R
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private enum class MarketSign(
    val symbol: String
) {
    UP("+"),
    DOWN("-"),
}

private val MarketSign.color: Color
    @Composable
    get() = when (this) {
        MarketSign.UP -> LocalColors.tradeUpRed
        MarketSign.DOWN -> LocalColors.tradeDownGreen
    }

enum class SortOrder(@param:StringRes val labelRes: Int) {
    CHANGE_RATE_DESC(R.string.sort_change_rate_desc),
    PRICE_DESC(R.string.sort_price_desc),
    NAME_ASC(R.string.sort_name_asc),
}

@Composable
internal fun MarketListContent(
    marketItems: List<MarketListContract.MarketItem>,
    sortOrder: SortOrder,
    searchQuery: String,
    onMarketClick: (market: String, marketType: MarketType, koreanName: String) -> Unit,
    pagerState: PagerState,
    marketTypes: List<MarketType>,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
    ) { page ->
        val pageMarketType = marketTypes[page]
        val listState = rememberLazyListState()

        val filteredItems by remember(marketItems, sortOrder, searchQuery, pageMarketType) {
            derivedStateOf {
                val normalizedQuery = searchQuery.trim()

                marketItems
                    .asSequence()
                    .filter { it.market.marketType == pageMarketType }
                    .filter { marketItem ->
                        normalizedQuery.isEmpty() ||
                            marketItem.market.koreanName.contains(normalizedQuery, ignoreCase = true) ||
                            marketItem.market.englishName.contains(normalizedQuery, ignoreCase = true) ||
                            marketItem.market.market.contains(normalizedQuery, ignoreCase = true)
                    }
                    .sortedWith(
                        when (sortOrder) {
                            SortOrder.PRICE_DESC -> compareByDescending { it.ticker.tradePrice }
                            SortOrder.CHANGE_RATE_DESC -> compareByDescending { it.ticker.signedChangeRate }
                            SortOrder.NAME_ASC -> compareBy { it.market.koreanName }
                        },
                    )
                    .toList()
            }
        }

        LaunchedEffect(sortOrder, searchQuery, page) {
            if (pagerState.currentPage == page) {
                listState.scrollToItem(0)
            }
        }

        if (filteredItems.isEmpty()) {
            EmptyContent(modifier = Modifier.fillMaxSize())
            return@HorizontalPager
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = filteredItems,
                key = { it.market.market },
                contentType = { "MarketRow" },
            ) { market ->
                MarketRow(
                    market = market,
                    onClick = {
                        onMarketClick(
                            market.market.market,
                            market.market.marketType,
                            market.market.koreanName,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MarketRow(
    market: MarketListContract.MarketItem,
    onClick: () -> Unit,
) {
    val extColors = LocalColors
    var lastPrice: Double by remember { mutableDoubleStateOf(market.ticker.tradePrice) }
    val highlightColor = remember { Animatable(Color.Transparent) }

    val sign = if (market.ticker.signedChangeRate >= 0) {
        MarketSign.UP
    } else {
        MarketSign.DOWN
    }

    LaunchedEffect(market.ticker.tradePrice) {
        val targetColor = when {
            market.ticker.tradePrice > lastPrice -> extColors.tradeUpRed
            market.ticker.tradePrice < lastPrice -> extColors.tradeDownGreen
            else -> null
        }

        lastPrice = market.ticker.tradePrice

        targetColor?.let { color ->
            highlightColor.animateTo(
                targetValue = color,
                animationSpec = tween(durationMillis = 300)
            )
            highlightColor.animateTo(
                targetValue = Color.Transparent,
                animationSpec = tween(durationMillis = 3000)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = highlightColor.value,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarketIcon(
                    market = market.market.market,
                    size = 40.dp
                )

                HorizontalSpacer(12.dp)

                Column {
                    Text(
                        text = market.market.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = market.market.koreanName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalSpacer(8.dp)

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = market.ticker.tradePrice.toPriceString(market.market.marketType),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "24h ${sign.symbol} ${market.ticker.signedChangeRate.toPercentString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = sign.color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun Double.toPriceString(marketType: MarketType): String = when (marketType) {
    MarketType.KRW -> NumberFormat.getNumberInstance(Locale.KOREA).format(this)
    else -> DECIMAL_PRICE_FORMAT.format(this)
}

private fun Double.toPercentString(): String = String.format(Locale.KOREA, "%.2f%%", abs(this * 100))

private val DECIMAL_PRICE_FORMAT = DecimalFormat("#,##0.#########")
