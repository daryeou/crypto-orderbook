package com.kwakwonjo.cryptoorderbook.feature.market.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kwakwonjo.cryptoorderbook.core.designsystem.component.HorizontalSpacer
import com.kwakwonjo.cryptoorderbook.feature.market.R

@Composable
internal fun TopBar(
    searchQuery: String,
    currentSortOrder: SortOrder,
    onChangeSearchQuery: (String) -> Unit,
    onChangeSortOrder: (SortOrder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo(isVisible = searchQuery.isEmpty())
        MarketSearchBar(
            modifier = Modifier.weight(1f),
            query = searchQuery,
            onQueryChange = { query -> onChangeSearchQuery(query) },
        )
        HorizontalSpacer(12.dp)
        SortMenu(
            currentSortOrder = currentSortOrder,
            onChangeSortOrder = { sortOrder -> onChangeSortOrder(sortOrder) },
        )
    }
}

@Composable
private fun Logo(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.padding(end = 12.dp),
        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
    ) {
        Text(
            text = stringResource(R.string.title_market_list),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        HorizontalSpacer(12.dp)
    }
}

@Composable
private fun SortMenu(
    currentSortOrder: SortOrder,
    onChangeSortOrder: (SortOrder) -> Unit,
) {
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isSortMenuExpanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.sort_button_description),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = isSortMenuExpanded,
            onDismissRequest = { isSortMenuExpanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
        ) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(order.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentSortOrder == order) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        onChangeSortOrder(order)
                        isSortMenuExpanded = false
                    },
                    leadingIcon = {
                        if (currentSortOrder == order) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}