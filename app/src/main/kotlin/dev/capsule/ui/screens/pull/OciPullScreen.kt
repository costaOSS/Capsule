package dev.capsule.ui.screens.pull

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OciPullScreen(
    onNavigateBack: () -> Unit,
    viewModel: OciPullViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var imageInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pull OCI Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = imageInput,
                onValueChange = { imageInput = it },
                label = { Text("Image (e.g. python:3.12-slim)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isPulling
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.pullImage(imageInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = imageInput.isNotBlank() && !uiState.isPulling
            ) {
                Text(if (uiState.isPulling) "Pulling..." else "Pull Image")
            }

            if (uiState.isPulling) {
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Layer ${uiState.currentLayer} of ${uiState.totalLayers}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.layers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Layers:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    itemsIndexed(uiState.layers) { index, layer ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (layer.length > 12) "${layer.take(12)}..."
                                    else layer
                                )
                            },
                            supportingContent = {
                                Text("Layer ${index + 1}")
                            }
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}