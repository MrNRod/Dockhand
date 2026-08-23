package com.mrnrod45.nstk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrnrod45.nstk.ui.viewmodels.RcmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcmScreen(
    viewModel: RcmViewModel
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("RCM Payload Injection") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Payload Selection",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.selectPayload() },
                            enabled = !viewModel.isBusy
                        ) {
                            Text("Select Payload (.bin)")
                        }
                        
                        Text(
                            text = viewModel.selectedPayload?.name ?: "No file selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (viewModel.selectedPayload != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Injection Controls
            Button(
                onClick = { viewModel.injectPayload() },
                enabled = viewModel.selectedPayload != null && !viewModel.isBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (viewModel.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Injecting...")
                } else {
                    Text("Inject Payload")
                }
            }

            // Logs
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logs",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                        IconButton(onClick = { viewModel.clearLog() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear Logs")
                        }
                    }
                    
                    HorizontalDivider()
                    
                    val logScrollState = rememberScrollState()
                    LaunchedEffect(viewModel.logText) {
                        logScrollState.scrollTo(logScrollState.maxValue)
                    }
                    SelectionContainer {
                        Text(
                            text = viewModel.logText,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(logScrollState),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
