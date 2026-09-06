package com.mrnrod45.dockhand.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.mrnrod45.dockhand.ui.components.AdaptiveActionButton
import com.mrnrod45.dockhand.ui.components.AdaptiveTopBar
import com.mrnrod45.dockhand.ui.components.SettingsGroup
import com.mrnrod45.dockhand.ui.components.SettingsToggleRow
import com.mrnrod45.dockhand.ui.components.rememberAdaptiveTopBarScrollBehavior
import com.mrnrod45.dockhand.ui.theme.LocalAppStyle
import com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val useSplitFiles by viewModel.useSplitFiles.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val useRomFolder by viewModel.useRomFolder.collectAsState()
    val allowXci by viewModel.allowXci.collectAsState()
    val validateIp by viewModel.validateIp.collectAsState()
    val expertMode by viewModel.expertMode.collectAsState()
    val expertHostIp by viewModel.expertHostIp.collectAsState()
    val expertHostPort by viewModel.expertHostPort.collectAsState()
    val expertHostExtra by viewModel.expertHostExtra.collectAsState()
    val expertNoRequestsServe by viewModel.expertNoRequestsServe.collectAsState()

    val style = LocalAppStyle.current
    val scrollBehavior = rememberAdaptiveTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = { AdaptiveTopBar("Settings", scrollBehavior) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = style.screenPadding)
                .padding(bottom = 24.dp)
        ) {
            SettingsGroup(title = "Main settings") {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (style.isOneUi) 20.dp else 0.dp,
                        vertical = 4.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language", modifier = Modifier.width(140.dp))
                    AdaptiveActionButton(
                        text = "English (en_US)",
                        onClick = { /* TODO: Language Picker */ },
                        enabled = false,
                        modifier = Modifier.width(180.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                AdaptiveActionButton(
                    text = "Change application font",
                    onClick = { /* TODO: Font Picker */ },
                    enabled = false,
                    modifier = Modifier.padding(
                        horizontal = if (style.isOneUi) 20.dp else 0.dp,
                        vertical = 4.dp
                    )
                )

                SettingsToggleRow(
                    title = "Auto check for updates",
                    checked = autoCheckUpdates,
                    onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) }
                )

                SettingsToggleRow(
                    title = "Split files larger than 4GB",
                    subtitle = "Keeps output compatible with FAT32 SD cards.",
                    checked = useSplitFiles,
                    onCheckedChange = { viewModel.toggleSplitFiles(it) }
                )

                SettingsToggleRow(
                    title = "Select a ROM folder instead of individual files",
                    subtitle = "Changes the 'Select files' button on the Games tab: choose a folder " +
                        "to add every supported file at once.",
                    checked = useRomFolder,
                    onCheckedChange = { viewModel.toggleUseRomFolder(it) }
                )
            }

            SettingsGroup(title = "File selection") {
                SettingsToggleRow(
                    title = "Allow XCI / NSZ / XCZ files",
                    subtitle = "When off, only *.nsp files can be selected regardless of protocol.",
                    checked = allowXci,
                    onCheckedChange = { viewModel.toggleAllowXci(it) }
                )

                SettingsToggleRow(
                    title = "Always validate NS IP input",
                    checked = validateIp,
                    onCheckedChange = { viewModel.toggleValidateIp(it) }
                )

                SettingsToggleRow(
                    title = "Expert mode (NET setup)",
                    checked = expertMode,
                    onCheckedChange = { viewModel.toggleExpertMode(it) }
                )
            }

            if (expertMode) {
                SettingsGroup(title = "Network transfer") {
                    SettingsToggleRow(
                        title = "Passive mode",
                        subtitle = "The app won't send a handshake to your Switch — trigger the " +
                            "connection from the Switch side.",
                        checked = expertNoRequestsServe,
                        onCheckedChange = { viewModel.toggleExpertNoRequestsServe(it) }
                    )

                    Column(
                        modifier = Modifier.padding(
                            horizontal = if (style.isOneUi) 20.dp else 0.dp,
                            vertical = 4.dp
                        )
                    ) {
                        OutlinedTextField(
                            value = expertHostIp,
                            onValueChange = { viewModel.setExpertHostIp(it.filter { c -> !c.isWhitespace() }) },
                            label = { Text("Host IP (blank = auto-detect)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            enabled = !expertNoRequestsServe
                        )

                        OutlinedTextField(
                            value = expertHostPort,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }
                                if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) <= 65535)
                                    viewModel.setExpertHostPort(filtered)
                            },
                            label = { Text("Host port (blank = 6042)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            enabled = !expertNoRequestsServe
                        )

                        OutlinedTextField(
                            value = expertHostExtra,
                            onValueChange = { viewModel.setExpertHostExtra(it.filter { c -> !c.isWhitespace() }) },
                            label = { Text("Extra URL path (optional)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            singleLine = true,
                            enabled = !expertNoRequestsServe
                        )
                        Text(
                            "Appended to each file URL in the handshake. Leave blank normally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
