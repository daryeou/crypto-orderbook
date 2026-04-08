package com.kwakwonjo.cryptoorderbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kwakwonjo.cryptoorderbook.navigation.CryptoOrderBookNavHost
import com.kwakwonjo.cryptoorderbook.theme.CryptoOrderBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CryptoOrderBookTheme {
                CryptoOrderBookNavHost()
            }
        }
    }
}


