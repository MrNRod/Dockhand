package com.mrnrod45.nstk.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.ui.viewmodels.UploadViewModel

import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.alpha

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel,
    viewModel: UploadViewModel = viewModel { UploadViewModel(usbController, filePicker, settingsViewModel) }
) {
    val files by viewModel.files.collectAsState()
    val selectedProtocol by viewModel.selectedProtocol.collectAsState()
    val transport by viewModel.transport.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    
    var isProtocolDropdownExpanded by remember { mutableStateOf(false) }
    var isTransportDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("NS-ToolKit") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { viewModel.startUpload() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    enabled = files.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (transport == "USB") "Upload to Switch" else "Upload to Network")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openFilePicker() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Files")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Protocol Selection
                        Column(modifier = Modifier.weight(1f).clickable { isProtocolDropdownExpanded = true }) {
                            Text("Protocol", style = MaterialTheme.typography.labelLarge)
                            Text(selectedProtocol, style = MaterialTheme.typography.headlineSmall)
                            
                            DropdownMenu(
                                expanded = isProtocolDropdownExpanded,
                                onDismissRequest = { isProtocolDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Goldleaf") },
                                    onClick = {
                                        viewModel.setProtocol("Goldleaf")
                                        isProtocolDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tinfoil (Awoo)") },
                                    onClick = {
                                        viewModel.setProtocol("Awoo")
                                        isProtocolDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tinfoil (Sphaira)") },
                                    onClick = {
                                        viewModel.setProtocol("Sphaira")
                                        isProtocolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))

                        // Transport Selection
                        // Only "Awoo" (Tinfoil Standard) supports Network install currently.
                        // Goldleaf and Sphaira are USB only.
                        val isTransportEnabled = selectedProtocol == "Awoo"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isTransportEnabled) { isTransportDropdownExpanded = true }
                                .alpha(if (isTransportEnabled) 1f else 0.5f)
                        ) {
                            Text("Transport", style = MaterialTheme.typography.labelLarge)
                            Text(transport, style = MaterialTheme.typography.headlineSmall)
                            
                            if (isTransportEnabled) {
                                DropdownMenu(
                                    expanded = isTransportDropdownExpanded,
                                onDismissRequest = { isTransportDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("USB") },
                                    onClick = {
                                        viewModel.setTransport("USB")
                                        isTransportDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("NET") },
                                    onClick = {
                                        viewModel.setTransport("NET")
                                        isTransportDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    }
                    
                    // IP Address Field (Only visible for NET)
                    if (transport == "NET") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { viewModel.setIpAddress(it) },
                            label = { Text("Switch IP Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // File List
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No files selected", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(files) { file ->
                        ListItem(
                            headlineContent = { Text(file.name) },
                            supportingContent = { Text(file.path) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeFile(file) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }
            
        }
    }
}

