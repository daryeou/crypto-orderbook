package com.kwakwonjo.cryptoorderbook.feature.market.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kwakwonjo.cryptoorderbook.core.designsystem.theme.CryptoAppTheme
import com.kwakwonjo.cryptoorderbook.feature.market.R

@Preview(name = "Search Empty", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun MarketSearchBarEmptyPreview() {
    PreviewTheme {
        MarketSearchBar(
            query = "",
            onQueryChange = {},
        )
    }
}

@Preview(name = "Search Filled", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun MarketSearchBarFilledPreview() {
    PreviewTheme {
        MarketSearchBar(
            query = "btc",
            onQueryChange = {},
        )
    }
}

@Preview(name = "Empty Content", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun EmptyContentPreview() {
    PreviewTheme {
        EmptyContent(modifier = Modifier)
    }
}

@Preview(name = "Error Content", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun ErrorContentPreview() {
    PreviewTheme {
        ErrorContent(
            titleRes = R.string.error_network_title,
            descriptionRes = R.string.error_network_desc,
            onRetry = {},
            modifier = Modifier,
        )
    }
}

@Preview(name = "Loading Content", showBackground = true, backgroundColor = 0xFF141416)
@Composable
private fun LoadingContentPreview() {
    PreviewTheme {
        LoadingContent(modifier = Modifier)
    }
}

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    CryptoAppTheme(content = content)
}
