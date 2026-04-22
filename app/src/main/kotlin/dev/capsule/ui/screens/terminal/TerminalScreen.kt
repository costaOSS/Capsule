package dev.capsule.ui.screens.terminal

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dev.capsule.ui.terminal.TerminalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    distroId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(distroId) {
        viewModel.startSession(distroId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.closeSession()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.distroName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidView(
                factory = { context ->
                    TerminalView(context).apply {
                        onDataReady = { data ->
                            viewModel.onTerminalOutput(data)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        }
    }
}