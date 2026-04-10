package com.kwakwonjo.cryptoorderbook.feature.market.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.LocalColors
import com.kwakwonjo.cryptoorderbook.core.model.MarketType
import com.kwakwonjo.cryptoorderbook.feature.market.MarketListContract
import com.kwakwonjo.cryptoorderbook.feature.market.R
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class MarketSign(val symbol: String, val color: @Composable () -> Color) {
    UP("▲", { LocalColors.tradeUpRed }),
    DOWN("▼", { LocalColors.tradeDownGreen })
}

enum class SortOrder(@StringRes val labelRes: Int) {
    CHANGE_RATE_DESC(R.string.sort_change_rate_desc),
    PRICE_DESC(R.string.sort_price_desc),
    NAME_ASC(R.string.sort_name_asc)
}

@Composable
internal fun MarketListContent(
    marketItems: List<MarketListContract.MarketItem>,
    sortOrder: SortOrder,
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

        val sortedItems by remember(marketItems, sortOrder, pageMarketType) {
            derivedStateOf {
                val filtered = marketItems.filter { it.info.marketType == pageMarketType }
                when (sortOrder) {
                    SortOrder.PRICE_DESC -> filtered.sortedByDescending { it.priceState.tradePrice }
                    SortOrder.CHANGE_RATE_DESC -> filtered.sortedByDescending { it.priceState.signedChangeRate }
                    SortOrder.NAME_ASC -> filtered.sortedBy { it.info.koreanName }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = sortedItems,
                key = { it.info.market },
                contentType = { "MarketRow" }
            ) { market ->
                MarketRow(
                    market = market,
                    onClick = {
                        onMarketClick(market.info.market, market.info.marketType, market.info.koreanName)
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
    val isUp = market.priceState.signedChangeRate >= 0
    val sign = if (isUp) MarketSign.UP else MarketSign.DOWN

    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 코인 심볼 및 이름
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 심볼 아이콘
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .size(40.dp),
                    model = market.info.icon,
                    contentDescription = "${market.info.englishName} icon",
                    loading = {
                        IconPlaceholder(market.info.symbol)
                    },
                    error = {
                        IconPlaceholder(market.info.symbol)
                    },
                )

                HorizontalSpacer(12.dp)

                Column {
                    Text(
                        text = market.info.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = market.info.koreanName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalSpacer(8.dp)

            // 오른쪽: 가격 및 변동률
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = market.priceState.tradePrice.toPriceString(market.info.marketType),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "24h ${sign.symbol} ${market.priceState.signedChangeRate.toPercentString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = sign.color(),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun IconPlaceholder(symbol: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 첫 글자 추출 (예: BTC -> B, safe 처리 포함)
        val firstChar = symbol.take(1).ifEmpty { "-" }

        Text(
            text = firstChar,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun Double.toPriceString(marketType: MarketType): String = when (marketType) {
    MarketType.KRW -> NumberFormat.getNumberInstance(Locale.KOREA).format(this)
    else -> DECIMAL_PRICE_FORMAT.format(this)
}

private fun Double.toPercentString(): String = String.format(Locale.KOREA, "%.2f%%", abs(this * 100))

private val DECIMAL_PRICE_FORMAT = DecimalFormat("#,##0.#########")