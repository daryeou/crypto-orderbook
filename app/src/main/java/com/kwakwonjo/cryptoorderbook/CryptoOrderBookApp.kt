package com.kwakwonjo.cryptoorderbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kwakwonjo.cryptoorderbook.core.model.NetworkAvailability
import com.kwakwonjo.cryptoorderbook.navigation.CryptoOrderBookNavHost

@Composable
fun CryptoOrderBookApp(
    onFinishRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppRootViewModel = hiltViewModel(),
) {
    val networkAvailability by viewModel.networkAvailability.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val interactionSource = remember { MutableInteractionSource() }
    val offlineSnackbarMessage = stringResource(R.string.global_offline_snackbar)
    val offlineOverlayMessage = stringResource(R.string.global_offline_overlay_message)
    var previousStatus by remember { mutableStateOf(networkAvailability) }

    LaunchedEffect(networkAvailability, offlineSnackbarMessage) {
        if (
            previousStatus == NetworkAvailability.CONNECTED &&
            networkAvailability == NetworkAvailability.DISCONNECTED
        ) {
            snackbarHostState.showSnackbar(
                message = offlineSnackbarMessage,
                duration = SnackbarDuration.Short,
            )
        }

        if (networkAvailability == NetworkAvailability.CONNECTED) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }

        previousStatus = networkAvailability
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CryptoOrderBookNavHost(
                modifier = Modifier.fillMaxSize(),
                onFinishRequest = onFinishRequest,
            )

            if (networkAvailability == NetworkAvailability.DISCONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {},
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = offlineOverlayMessage,
                            modifier = Modifier.padding(top = 16.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
