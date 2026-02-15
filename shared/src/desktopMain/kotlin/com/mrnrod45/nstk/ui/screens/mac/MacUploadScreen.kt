package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.ui.components.MacButton
import com.mrnrod45.nstk.ui.components.MacDivider
import com.mrnrod45.nstk.ui.components.MacDropdown
import com.mrnrod45.nstk.ui.components.MacGroup
import com.mrnrod45.nstk.ui.components.MacIconButton
import com.mrnrod45.nstk.ui.components.MacLabel
import com.mrnrod45.nstk.ui.components.MacTextField
import com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel
import com.mrnrod45.nstk.ui.viewmodels.UploadViewModel

@Composable
fun MacUploadScreen(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: SettingsViewModel,
    viewModel: UploadViewModel = viewModel { UploadViewModel(usbController, filePicker, settingsViewModel) }
) {
    val files by viewModel.files.collectAsState()
    val selectedProtocol by viewModel.selectedProtocol.collectAsState()
    val transport by viewModel.transport.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    val useRomFolder by settingsViewModel.useRomFolder.collectAsState()

    var isProtocolDropdownExpanded by remember { mutableStateOf(false) }
    var isTransportDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header / Title ---
        Text(
            text = "Upload Files",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // --- Configuration Area ---
        MacGroup(title = "Connection Settings") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Protocol Selector
                Column(modifier = Modifier.weight(1f)) {
                    MacLabel("Protocol", modifier = Modifier.padding(bottom = 4.dp))
                    MacDropdown(
                        items = listOf("Goldleaf", "Awoo", "Sphaira"),
                        selectedItem = selectedProtocol,
                        onItemSelected = { viewModel.setProtocol(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Transport Selector
                val isTransportEnabled = selectedProtocol == "Awoo"
                Column(modifier = Modifier.weight(1f)) {
                    MacLabel("Transport", modifier = Modifier.padding(bottom = 4.dp))
                    MacDropdown(
                        items = listOf("USB", "NET"),
                        selectedItem = transport,
                        onItemSelected = { viewModel.setTransport(it) },
                        enabled = isTransportEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // IP Address (Conditional)
            if (transport == "NET") {
                Spacer(modifier = Modifier.height(12.dp))
                MacLabel("Nintendo Switch IP Address", modifier = Modifier.padding(bottom = 4.dp))
                MacTextField(
                    value = ipAddress,
                    onValueChange = { viewModel.setIpAddress(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- File Management Area ---
        Column(modifier = Modifier.weight(1f)) {
            MacLabel("Selected Files", modifier = Modifier.padding(bottom = 6.dp))
            
            // Mac-style List Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No files added", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(files) { file ->
                            // Simple row item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name, 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Medium, 
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 13.sp
                                    )
                                    Text(
                                        text = file.path, 
                                        fontSize = 11.sp, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        maxLines = 1,
                                        lineHeight = 11.sp
                                    )
                                }
                                MacIconButton(
                                    onClick = { viewModel.removeFile(file) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (files.indexOf(file) < files.size - 1) {
                                MacDivider(modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // --- Bottom Actions ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add File Button (Secondary)
            MacButton(onClick = { viewModel.openFilePicker() }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (useRomFolder) "Add Folder" else "Add Files", fontSize = 13.sp, lineHeight = 13.sp)
            }
            
            // Upload Button (Primary)
            MacButton(
                onClick = { viewModel.startUpload() },
                enabled = files.isNotEmpty(),
                primary = true
            ) {
                Text(
                    text = if (transport == "USB") "Upload to Switch" else "Upload to Network",
                    fontSize = 13.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
