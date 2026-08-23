package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
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
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // --- Connection Settings ---
        MacGroup(title = "Connection") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MacLabel("Protocol", modifier = Modifier.padding(bottom = 5.dp))
                    MacDropdown(
                        items = listOf("Goldleaf", "Awoo", "Sphaira"),
                        selectedItem = selectedProtocol,
                        onItemSelected = { viewModel.setProtocol(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val isTransportEnabled = selectedProtocol == "Awoo"
                Column(modifier = Modifier.weight(1f)) {
                    MacLabel("Transport", modifier = Modifier.padding(bottom = 5.dp))
                    MacDropdown(
                        items = listOf("USB", "NET"),
                        selectedItem = transport,
                        onItemSelected = { viewModel.setTransport(it) },
                        enabled = isTransportEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (transport == "NET") {
                Spacer(Modifier.height(10.dp))
                MacLabel("Switch IP Address", modifier = Modifier.padding(bottom = 5.dp))
                MacTextField(
                    value = ipAddress,
                    onValueChange = { viewModel.setIpAddress(it) },
                    placeholder = "192.168.1.xxx",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- File List ---
        Column(modifier = Modifier.weight(1f)) {
            MacLabel("Selected Files", modifier = Modifier.padding(bottom = 6.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                if (files.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No files added\nClick \"Add Files\" to get started",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(files) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp
                                    )
                                    Text(
                                        text = file.path,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        lineHeight = 13.sp
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                MacIconButton(
                                    onClick = { viewModel.removeFile(file) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (files.indexOf(file) < files.size - 1) {
                                MacDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- Logs ---
        if (logs.isNotBlank()) {
            Column(modifier = Modifier.height(110.dp)) {
                MacLabel("Logs", modifier = Modifier.padding(bottom = 6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val logScrollState = rememberScrollState()
                    LaunchedEffect(logs) {
                        logScrollState.scrollTo(logScrollState.maxValue)
                    }
                    Text(
                        text = logs,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(logScrollState),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // --- Bottom Action Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacButton(onClick = { viewModel.openFilePicker() }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (useRomFolder) "Add Folder…" else "Add Files…", fontSize = 13.sp, lineHeight = 13.sp)
            }

            MacButton(
                onClick = { viewModel.startUpload() },
                enabled = files.isNotEmpty(),
                primary = true
            ) {
                Text(
                    text = if (transport == "USB") "Upload to Switch" else "Upload over Network",
                    fontSize = 13.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
