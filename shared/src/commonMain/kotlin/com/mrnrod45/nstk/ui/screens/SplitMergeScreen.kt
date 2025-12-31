package com.mrnrod45.nstk.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.ui.viewmodels.SplitMergeViewModel
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.domain.file.FileSplitter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitMergeScreen(
    filePicker: FilePicker,
    fileSplitter: FileSplitter,
    viewModel: SplitMergeViewModel = viewModel { SplitMergeViewModel(filePicker, fileSplitter) }
) {
    val isSplitMode by viewModel.isSplitMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val outputPath by viewModel.outputPath.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Split & merge files tool") },
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
            // TOP SECTION: Controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSplitMode,
                                onClick = { viewModel.setSplitMode(true) }
                            )
                            Text("Split")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isSplitMode,
                                onClick = { viewModel.setSplitMode(false) }
                            )
                            Text("Merge")
                        }
                    }

                    // File Selection Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.selectFile() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSplitMode) "Select File" else "Add Files")
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.clearSelection() },
                            enabled = selectedPaths.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                    }
                    
                    // Output Path Selection
                    Column {
                         Text("Save to:", style = MaterialTheme.typography.bodyMedium)
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             Text(
                                 outputPath, 
                                 style = MaterialTheme.typography.bodySmall, 
                                 color = Color.Gray,
                                 modifier = Modifier.weight(1f)
                             )
                             OutlinedButton(
                                onClick = { viewModel.changeOutputPath() }
                            ) {
                                Text("Change")
                            }
                         }
                    }
                }
            }

            // MIDDLE SECTION: File List / Visualization
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    .padding(8.dp),
                contentAlignment = if (selectedPaths.isEmpty()) Alignment.Center else Alignment.TopStart
            ) {
                 if (selectedPaths.isEmpty()) {
                     Text("No file selected", color = Color.Gray)
                 } else {
                     androidx.compose.foundation.lazy.LazyColumn {
                         items(selectedPaths.size) { index ->
                             Text(
                                 text = selectedPaths[index],
                                 style = MaterialTheme.typography.bodyMedium,
                                 modifier = Modifier.padding(vertical = 4.dp)
                             )
                             if (index < selectedPaths.size - 1) {
                                 androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                             }
                         }
                     }
                 }
            }
            
            // Status Message
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, style = MaterialTheme.typography.labelMedium)
            }

            // BOTTOM SECTION: Action Button
            Button(
                onClick = { viewModel.startConversion() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedPaths.isNotEmpty() && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Keep Green
            ) {
                if (isProcessing) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Convert", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
