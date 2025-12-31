package com.mrnrod45.nstk.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel
import PlatformType
import getPlatformType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val useSplitFiles by viewModel.useSplitFiles.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val useRomFolder by viewModel.useRomFolder.collectAsState()
    val showOnlyNsp by viewModel.showOnlyNsp.collectAsState()
    val goldLeafVersion by viewModel.goldLeafVersion.collectAsState()
    val allowXci by viewModel.allowXci.collectAsState()
    val validateIp by viewModel.validateIp.collectAsState()
    val expertMode by viewModel.expertMode.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val themeConfig by viewModel.themeConfig.collectAsState()
            val platformType = getPlatformType()
            
            // Reusable Dropdown Component
            @Composable
            fun <T> SettingsDropdown(
                label: String,
                selectedValue: String,
                items: List<Pair<String, T>>,
                onItemSelected: (T) -> Unit
            ) {
                var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = selectedValue,
                            onValueChange = {},
                            readOnly = true,


                            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            items.forEach { (text, item) ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(text) },
                                    onClick = {
                                        onItemSelected(item)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Main Settings Section
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 1. Desktop Theme Mode (Android uses combined dropdown below)
            if (platformType == PlatformType.DESKTOP) {
                SettingsDropdown(
                    label = "Theme Mode",
                    selectedValue = when(themeConfig.mode) {
                        com.mrnrod45.nstk.ui.theme.ThemeMode.SYSTEM -> "Follow System"
                        com.mrnrod45.nstk.ui.theme.ThemeMode.LIGHT -> "Light"
                        com.mrnrod45.nstk.ui.theme.ThemeMode.DARK -> "Dark"
                    },
                    items = listOf(
                        "Follow System" to com.mrnrod45.nstk.ui.theme.ThemeMode.SYSTEM,
                        "Light" to com.mrnrod45.nstk.ui.theme.ThemeMode.LIGHT,
                        "Dark" to com.mrnrod45.nstk.ui.theme.ThemeMode.DARK
                    ),
                    onItemSelected = { viewModel.setThemeMode(it) }
                )
            }

            // 2. Android Specifics
            if (platformType == PlatformType.ANDROID) {
                Spacer(Modifier.height(8.dp))
                
                // Define Theme Presets
                data class ThemePreset(
                    val label: String,
                    val mode: com.mrnrod45.nstk.ui.theme.ThemeMode,
                    val darkConfig: com.mrnrod45.nstk.ui.theme.DarkThemeConfig,
                    val dynamic: Boolean
                )
                
                val presets = listOf(
                    ThemePreset("Light", com.mrnrod45.nstk.ui.theme.ThemeMode.LIGHT, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, false),
                    ThemePreset("Dark", com.mrnrod45.nstk.ui.theme.ThemeMode.DARK, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, false),
                    ThemePreset("Black", com.mrnrod45.nstk.ui.theme.ThemeMode.DARK, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.AMOLED, false),
                    ThemePreset("System", com.mrnrod45.nstk.ui.theme.ThemeMode.SYSTEM, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, false),
                    ThemePreset("System Black", com.mrnrod45.nstk.ui.theme.ThemeMode.SYSTEM, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.AMOLED, false),
                    ThemePreset("System Dynamic", com.mrnrod45.nstk.ui.theme.ThemeMode.SYSTEM, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, true),
                    ThemePreset("Light Dynamic", com.mrnrod45.nstk.ui.theme.ThemeMode.LIGHT, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, true),
                    ThemePreset("Dark with Dynamic", com.mrnrod45.nstk.ui.theme.ThemeMode.DARK, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.STANDARD, true),
                    ThemePreset("Black with Dynamic", com.mrnrod45.nstk.ui.theme.ThemeMode.DARK, com.mrnrod45.nstk.ui.theme.DarkThemeConfig.AMOLED, true)
                )
                
                // Determine current label
                val currentPreset = presets.find { 
                    it.mode == themeConfig.mode && 
                    it.darkConfig == themeConfig.darkThemeConfig && 
                    it.dynamic == themeConfig.useDynamicColor 
                } ?: presets.first() // Fallback
                
                SettingsDropdown(
                    label = "Theme Options",
                    selectedValue = currentPreset.label,
                    items = presets.map { it.label to it },
                    onItemSelected = { preset ->
                        viewModel.setThemeMode(preset.mode)
                        viewModel.setDarkThemeConfig(preset.darkConfig)
                        viewModel.toggleDynamicColor(preset.dynamic)
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Main settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Language Selection (Placeholder)
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Language", modifier = Modifier.width(100.dp))
                Button(onClick = { /* TODO: Language Picker */ }) {
                    Text("English (en_US)")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { /* TODO: Confirm Language */ }) {
                    Text("OK")
                }
            }

            // Font Button
            Button(
                onClick = { /* TODO: Font Picker */ },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text("Change application font")
            }

            // Auto Check Updates
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = autoCheckUpdates,
                    onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) }
                )
                Text("Auto check for updates")
            }
            
            Spacer(Modifier.height(16.dp))

            // ROM Folder Option
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = useRomFolder,
                    onCheckedChange = { viewModel.toggleUseRomFolder(it) }
                )
                Text("Select folder with ROM files instead of selecting ROMs individually.")
            }
            Text(
                "Changes 'Select files' button behaviour on 'Games' tab: instead of selecting ROM files one-by-one you can choose folder to add every supported file at once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
            )

            HorizontalDivider()

            // Goldleaf Section
            Text(
                "Goldleaf Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showOnlyNsp,
                    onCheckedChange = { viewModel.toggleShowOnlyNsp(it) }
                )
                Text("Show only *.nsp in Goldleaf.")
            }
            
             // Goldleaf Version (Placeholder)
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Goldleaf version", modifier = Modifier.width(120.dp))
                Button(onClick = { /* TODO: Version Picker */ }) {
                    Text(goldLeafVersion)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Awoo Section
            Text(
                "Awoo Installer and compatible",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = allowXci,
                    onCheckedChange = { viewModel.toggleAllowXci(it) }
                )
                Text("Allow XCI / NSZ / XCZ files selection for Awoo/TinWoo Installer and Sphaira.")
            }
             Text(
                "Used by applications that support XCI/NSZ/XCZ and utilizes Tinfoil (aka Awoo/TinWoo/Sphaira) transfer protocol. Don't change if not sure. Enable for Awoo/TinWoo Installer and Sphaira.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = validateIp,
                    onCheckedChange = { viewModel.toggleValidateIp(it) }
                )
                Text("Always validate NS IP input.")
            }

             Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = expertMode,
                    onCheckedChange = { viewModel.toggleExpertMode(it) }
                )
                Text("Expert mode (NET setup)")
            }
        }
    }
}
