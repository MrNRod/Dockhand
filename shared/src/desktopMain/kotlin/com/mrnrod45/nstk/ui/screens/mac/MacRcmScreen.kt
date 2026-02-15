package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.components.MacButton
import com.mrnrod45.nstk.ui.components.MacGroup
import com.mrnrod45.nstk.ui.components.MacLabel
import com.mrnrod45.nstk.ui.viewmodels.RcmViewModel

@Composable
fun MacRcmScreen(
    viewModel: RcmViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Text(
            text = "RCM Payload Injection",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // --- Payload Selection ---
        MacGroup(title = "Payload") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacButton(
                    onClick = { viewModel.selectPayload() },
                    enabled = !viewModel.isBusy
                ) {
                    Text("Select Payload (.bin)")
                }
                
                Text(
                    text = viewModel.selectedPayload?.name ?: "No payload selected",
                    fontSize = 13.sp,
                    color = if (viewModel.selectedPayload != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MacButton(
                    onClick = { viewModel.injectPayload() },
                    enabled = viewModel.selectedPayload != null && !viewModel.isBusy,
                    primary = true
                ) {
                    if (viewModel.isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Injecting...")
                    } else {
                        Text("Inject Payload")
                    }
                }
            }
        }

        // --- Logs ---
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacLabel("Injection Logs")
                MacButton(onClick = { viewModel.clearLog() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
            
            // Console-like Log Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = viewModel.logText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
