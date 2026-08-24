package com.mrnrod45.nstk.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    // Placeholder settings for now. In a real app, this would be backed by Preferences.
    private val _useSplitFiles = MutableStateFlow(true)
    val useSplitFiles: StateFlow<Boolean> = _useSplitFiles.asStateFlow()

    private val _autoCheckUpdates = MutableStateFlow(true)
    val autoCheckUpdates: StateFlow<Boolean> = _autoCheckUpdates.asStateFlow()

    // Main Settings
    private val _selectedLanguage = MutableStateFlow("English (en_US)")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()
    
    private val _useRomFolder = MutableStateFlow(false)
    val useRomFolder: StateFlow<Boolean> = _useRomFolder.asStateFlow()

    // Goldleaf Settings
    private val _showOnlyNsp = MutableStateFlow(false)
    val showOnlyNsp: StateFlow<Boolean> = _showOnlyNsp.asStateFlow()

    private val _goldleafHost = MutableStateFlow("192.168.1.1")
    
    private val _goldLeafVersion = MutableStateFlow("v0.10+") // Default/Placeholder
    val goldLeafVersion: StateFlow<String> = _goldLeafVersion.asStateFlow()

    // Awoo Settings
    private val _allowXci = MutableStateFlow(true) // Checked by default in screenshot
    val allowXci: StateFlow<Boolean> = _allowXci.asStateFlow()

    private val _validateIp = MutableStateFlow(true) // Checked by default
    val validateIp: StateFlow<Boolean> = _validateIp.asStateFlow()

    private val _expertMode = MutableStateFlow(false)
    val expertMode: StateFlow<Boolean> = _expertMode.asStateFlow()

    // Expert Mode sub-settings (NET transfer, mirrors original SettingsBlockTinfoilController)
    private val _expertHostIp = MutableStateFlow("")          // blank = auto-detect
    val expertHostIp: StateFlow<String> = _expertHostIp.asStateFlow()

    private val _expertHostPort = MutableStateFlow("")        // blank = use default 6042
    val expertHostPort: StateFlow<String> = _expertHostPort.asStateFlow()

    private val _expertHostExtra = MutableStateFlow("")       // extra URL path suffix
    val expertHostExtra: StateFlow<String> = _expertHostExtra.asStateFlow()

    private val _expertNoRequestsServe = MutableStateFlow(false) // passive mode: switch connects to us
    val expertNoRequestsServe: StateFlow<Boolean> = _expertNoRequestsServe.asStateFlow()

    // Theme Settings
    private val _themeConfig = MutableStateFlow(com.mrnrod45.nstk.ui.theme.AppThemeConfig())
    val themeConfig: StateFlow<com.mrnrod45.nstk.ui.theme.AppThemeConfig> = _themeConfig.asStateFlow()

    fun toggleSplitFiles(enabled: Boolean) {
        _useSplitFiles.value = enabled
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        _autoCheckUpdates.value = enabled
    }
    
    fun setLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun toggleUseRomFolder(enabled: Boolean) {
        _useRomFolder.value = enabled
    }

    fun toggleShowOnlyNsp(enabled: Boolean) {
        _showOnlyNsp.value = enabled
    }

    fun setGoldLeafVersion(version: String) {
        _goldLeafVersion.value = version
    }

    fun toggleAllowXci(enabled: Boolean) {
        _allowXci.value = enabled
    }

    fun toggleValidateIp(enabled: Boolean) {
        _validateIp.value = enabled
    }
    
    fun toggleExpertMode(enabled: Boolean) {
        _expertMode.value = enabled
    }

    fun setExpertHostIp(ip: String) { _expertHostIp.value = ip }
    fun setExpertHostPort(port: String) { _expertHostPort.value = port }
    fun setExpertHostExtra(extra: String) { _expertHostExtra.value = extra }
    fun toggleExpertNoRequestsServe(enabled: Boolean) { _expertNoRequestsServe.value = enabled }

    fun setThemeMode(mode: com.mrnrod45.nstk.ui.theme.ThemeMode) {
        _themeConfig.value = _themeConfig.value.copy(mode = mode)
    }

    fun setDarkThemeConfig(config: com.mrnrod45.nstk.ui.theme.DarkThemeConfig) {
        _themeConfig.value = _themeConfig.value.copy(darkThemeConfig = config)
    }

    fun toggleDynamicColor(enabled: Boolean) {
        _themeConfig.value = _themeConfig.value.copy(useDynamicColor = enabled)
    }
}
