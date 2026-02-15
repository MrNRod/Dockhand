package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.ui.components.MacButton
import com.mrnrod45.nstk.ui.components.MacDivider
import com.mrnrod45.nstk.ui.components.MacGroup
import com.mrnrod45.nstk.ui.components.MacLabel
import com.mrnrod45.nstk.ui.components.MacRadioButton
import com.mrnrod45.nstk.ui.viewmodels.SplitMergeViewModel

@Composable
fun MacSplitMergeScreen(
    filePicker: FilePicker,
    fileSplitter: FileSplitter,
    viewModel: SplitMergeViewModel = viewModel { SplitMergeViewModel(filePicker, fileSplitter) }
) {
    val isSplitMode by viewModel.isSplitMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val outputPath by viewModel.outputPath.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Text(
            text = "Split & Merge",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // --- Controls ---
        MacGroup(title = "Operation") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacRadioButton(
                        selected = isSplitMode,
                        onClick = { viewModel.setSplitMode(true) },
                        modifier = Modifier.padding(end = 8.dp) // Add spacing between radio and text
                    )
                    Text(
                        text = "Split", 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSurface,
                        style = LocalTextStyle.current.copy(lineHeight = 13.sp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MacRadioButton(
                        selected = !isSplitMode,
                        onClick = { viewModel.setSplitMode(false) },
                        modifier = Modifier.padding(end = 8.dp) // Add spacing between radio and text
                    )
                    Text(
                        text = "Merge", 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSurface,
                        style = LocalTextStyle.current.copy(lineHeight = 13.sp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacButton(
                    onClick = { viewModel.selectFile() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSplitMode) "Select File..." else "Add Files...")
                }
                
                MacButton(
                    onClick = { viewModel.clearSelection() },
                    enabled = selectedPaths.isNotEmpty()
                ) {
                    Text("Clear")
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Output path
            Row(verticalAlignment = Alignment.CenterVertically) {
                MacLabel(
                    text = "Save to: ", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = outputPath,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                MacButton(onClick = { viewModel.changeOutputPath() }) {
                    Text("Change...")
                }
            }
        }

        // --- File List ---
        Column(modifier = Modifier.weight(1f)) {
            MacLabel("Files to Process")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (selectedPaths.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No file selected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(selectedPaths.size) { index ->
                            Text(
                                text = selectedPaths[index],
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            if (index < selectedPaths.size - 1) {
                                MacDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
                            }
                        }
                    }
                }
            }
        }
        
        // --- Status ---
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                fontSize = 12.sp,
                color = if (statusMessage.contains("Success")) Color(0xFF2E7D32) else Color.Red
            )
        }

        // --- Action ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MacButton(
                onClick = { viewModel.startConversion() },
                enabled = selectedPaths.isNotEmpty() && !isProcessing,
                primary = true,
                modifier = Modifier.width(120.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Processing")
                } else {
                    Text("Convert")
                }
            }
        }
    }
}
