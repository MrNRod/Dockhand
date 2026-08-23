package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.ui.components.MacButton
import com.mrnrod45.nstk.ui.components.MacDivider
import com.mrnrod45.nstk.ui.components.MacGroup
import com.mrnrod45.nstk.ui.components.MacLabel
import com.mrnrod45.nstk.ui.components.MacSegmentedControl
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // --- Operation ---
        MacGroup(title = "Operation") {

            // Mode selector
            MacSegmentedControl(
                items = listOf("Split", "Merge"),
                selectedIndex = if (isSplitMode) 0 else 1,
                onSegmentSelected = { index -> viewModel.setSplitMode(index == 0) },
                modifier = Modifier.width(180.dp)
            )

            Spacer(Modifier.height(10.dp))

            // File selector buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacButton(
                    onClick = { viewModel.selectFile() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSplitMode) "Select File…" else "Add Files…", fontSize = 13.sp)
                }
                MacButton(
                    onClick = { viewModel.clearSelection() },
                    enabled = selectedPaths.isNotEmpty()
                ) {
                    Text("Clear", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Output path
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacLabel(
                    text = "Save to:",
                    modifier = Modifier.widthIn(min = 52.dp)
                )
                Text(
                    text = outputPath,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                MacButton(onClick = { viewModel.changeOutputPath() }) {
                    Text("Change…", fontSize = 13.sp)
                }
            }
        }

        // --- File List ---
        Column(modifier = Modifier.weight(1f)) {
            MacLabel("Files to Process", modifier = Modifier.padding(bottom = 6.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                if (selectedPaths.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No files selected",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(selectedPaths.size) { index ->
                            Text(
                                text = selectedPaths[index],
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            if (index < selectedPaths.size - 1) {
                                MacDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Status Badge ---
        if (statusMessage.isNotEmpty()) {
            val isSuccess = statusMessage.contains("success", ignoreCase = true)
            val bgColor = if (isSuccess) Color(0xFF1B5E20).copy(alpha = 0.12f) else Color(0xFFB71C1C).copy(alpha = 0.10f)
            val textColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = statusMessage, fontSize = 12.sp, lineHeight = 15.sp, color = textColor)
            }
        }

        // --- Action ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MacButton(
                onClick = { viewModel.startConversion() },
                enabled = selectedPaths.isNotEmpty() && !isProcessing,
                primary = true,
                modifier = Modifier.widthIn(min = 120.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Processing…", fontSize = 13.sp)
                } else {
                    Text(if (isSplitMode) "Split" else "Merge", fontSize = 13.sp)
                }
            }
        }
    }
}
