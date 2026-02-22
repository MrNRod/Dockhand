package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // --- Payload Section ---
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
                    Text("Select Payload (.bin)", fontSize = 13.sp)
                }

                Text(
                    text = viewModel.selectedPayload?.name ?: "No payload selected",
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    color = if (viewModel.selectedPayload != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

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
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Injecting…", fontSize = 13.sp)
                    } else {
                        Text("Inject Payload", fontSize = 13.sp)
                    }
                }
            }
        }

        // --- Log Section ---
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacLabel("Injection Log")
                MacButton(onClick = { viewModel.clearLog() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp)
                }
            }

            // Console-style log box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                val scroll = rememberScrollState(Int.MAX_VALUE)
                Text(
                    text = viewModel.logText.ifBlank { "Logs will appear here after injection." },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = if (viewModel.logText.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
