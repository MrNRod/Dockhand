package com.mrnrod45.nstk.ui.screens.mac

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.components.MacCheckbox
import com.mrnrod45.nstk.ui.components.MacDropdown
import com.mrnrod45.nstk.ui.components.MacGroup
import com.mrnrod45.nstk.ui.components.MacLabel
import com.mrnrod45.nstk.ui.components.MacTextField
import com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel
import com.mrnrod45.nstk.ui.theme.ThemeMode

@Composable
fun MacSettingsScreen(
    viewModel: SettingsViewModel
) {
    val themeConfig by viewModel.themeConfig.collectAsState()
    val useSplitFiles by viewModel.useSplitFiles.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val useRomFolder by viewModel.useRomFolder.collectAsState()
    val showOnlyNsp by viewModel.showOnlyNsp.collectAsState()
    val goldLeafVersion by viewModel.goldLeafVersion.collectAsState()
    val allowXci by viewModel.allowXci.collectAsState()
    val validateIp by viewModel.validateIp.collectAsState()
    val expertMode by viewModel.expertMode.collectAsState()
    val expertHostIp by viewModel.expertHostIp.collectAsState()
    val expertHostPort by viewModel.expertHostPort.collectAsState()
    val expertHostExtra by viewModel.expertHostExtra.collectAsState()
    val expertNoRequestsServe by viewModel.expertNoRequestsServe.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // --- Appearance ---
        MacGroup(title = "Appearance") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Theme Mode", 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.onSurface,
                    style = LocalTextStyle.current.copy(lineHeight = 13.sp)
                )
                
                MacDropdown(
                    items = listOf("System Default", "Light", "Dark"),
                    selectedItem = when(themeConfig.mode) {
                        ThemeMode.SYSTEM -> "System Default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    onItemSelected = { item ->
                        val mode = when(item) {
                            "Light" -> ThemeMode.LIGHT
                            "Dark" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                        viewModel.setThemeMode(mode)
                    },
                    modifier = Modifier.width(140.dp)
                )
            }
        }

        // --- General ---
        MacGroup(title = "General") {
            MacCheckboxRow(
                label = "Auto-check for updates on launch",
                checked = autoCheckUpdates,
                onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) }
            )

            MacCheckboxRow(
                label = "Split files larger than 4GB (FAT32 compatibility)",
                checked = useSplitFiles,
                onCheckedChange = { viewModel.toggleSplitFiles(it) }
            )

            MacCheckboxRow(
                label = "Use ROM folder select mode",
                checked = useRomFolder,
                onCheckedChange = { viewModel.toggleUseRomFolder(it) }
            )
            MacLabel(
                "Select a folder containing ROMs instead of selecting individual files.",
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )
        }

        // --- Goldleaf ---
        MacGroup(title = "Goldleaf") {
            MacCheckboxRow(
                label = "Show only .nsp files",
                checked = showOnlyNsp,
                onCheckedChange = { viewModel.toggleShowOnlyNsp(it) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacLabel("Goldleaf Version", color = MaterialTheme.colorScheme.onSurface)
                MacDropdown(
                    items = listOf("v0.5", "v0.7.x", "v0.8-0.9", "v0.10+"),
                    selectedItem = goldLeafVersion,
                    onItemSelected = { viewModel.setGoldLeafVersion(it) },
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        // --- Awoo / Tinfoil ---
        MacGroup(title = "Awoo / Tinfoil / Sphaira") {
            MacCheckboxRow(
                label = "Allow XCI / NSZ / XCZ selection",
                checked = allowXci,
                onCheckedChange = { viewModel.toggleAllowXci(it) }
            )
            MacLabel(
                "Enable for installers that support compressed formats.",
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )

            MacCheckboxRow(
                label = "Always validate IP address format",
                checked = validateIp,
                onCheckedChange = { viewModel.toggleValidateIp(it) }
            )

            MacCheckboxRow(
                label = "Expert Mode (NET)",
                checked = expertMode,
                onCheckedChange = { viewModel.toggleExpertMode(it) }
            )

            // Expert mode sub-fields, shown only when expert mode is on
            if (expertMode) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacCheckboxRow(
                        label = "Passive mode (don\u2019t send handshake to Switch)",
                        checked = expertNoRequestsServe,
                        onCheckedChange = { viewModel.toggleExpertNoRequestsServe(it) }
                    )
                    MacLabel(
                        "When enabled the app won\u2019t send a handshake — you trigger the transfer from the Switch.",
                        modifier = Modifier.padding(start = 21.dp)
                    )

                    MacTextField(
                        value = expertHostIp,
                        onValueChange = { v -> viewModel.setExpertHostIp(v.filter { c -> !c.isWhitespace() }) },
                        placeholder = "Host IP  (blank = auto-detect)",
                        enabled = !expertNoRequestsServe,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MacTextField(
                        value = expertHostPort,
                        onValueChange = { v ->
                            val f = v.filter { c -> c.isDigit() }
                            if (f.isEmpty() || (f.toIntOrNull() ?: 0) <= 65535) viewModel.setExpertHostPort(f)
                        },
                        placeholder = "Host Port  (blank = 6042)",
                        enabled = !expertNoRequestsServe,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MacTextField(
                        value = expertHostExtra,
                        onValueChange = { v -> viewModel.setExpertHostExtra(v.filter { c -> !c.isWhitespace() }) },
                        placeholder = "Extra URL path  (optional)",
                        enabled = !expertNoRequestsServe,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MacLabel(
                        "Appended to each file URL in the handshake. Leave blank normally.",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MacCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MacCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            style = LocalTextStyle.current.copy(lineHeight = 13.sp)
        )
    }
}
