package com.mrnrod45.nstk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.domain.models.UnifiedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.mrnrod45.nstk.platform.getDefaultDownloadsPath

class SplitMergeViewModel(
    private val filePicker: FilePicker,
    private val fileSplitter: FileSplitter,
    private val settingsViewModel: SettingsViewModel
) : ViewModel() {
    private val _isSplitMode = MutableStateFlow(true)
    val isSplitMode: StateFlow<Boolean> = _isSplitMode.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<UnifiedFile>>(emptyList())
    // Expose list of paths for UI visualization
    val selectedPaths: StateFlow<List<String>> = _selectedFiles.map { list -> 
        list.map { it.path } 
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Kept for compatibility if UI only checks for null/empty, but suggest using selectedPaths
    val selectedPath: StateFlow<String?> = _selectedFiles.map { it.firstOrNull()?.path }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    private val _outputPath = MutableStateFlow(getDefaultDownloadsPath()) // Default
    val outputPath: StateFlow<String> = _outputPath.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setSplitMode(isSplit: Boolean) {
        _isSplitMode.value = isSplit
        // Optional: Clear selection on mode switch if desired, but user might want to keep
    }

    fun selectFile() {
        viewModelScope.launch {
            // Split mode: picking a game file to split, so filter it like Upload does
            // (.nsp always, XCI/NSZ/XCZ only when allowXci is on). Merge mode picks split
            // chunks instead (e.g. "game.nsp.00"), which don't carry those extensions, so
            // it stays unfiltered.
            val allowedExtensions = if (_isSplitMode.value) {
                val extensions = mutableListOf("nsp")
                if (settingsViewModel.allowXci.value) extensions += listOf("xci", "nsz", "xcz")
                extensions
            } else {
                emptyList()
            }
            val files = filePicker.pickFiles(allowedExtensions)
            if (files.isNotEmpty()) {
                if (_isSplitMode.value) {
                    // Split Mode: User requested single item only (overwrite)
                    _selectedFiles.value = listOf(files.first())
                    _statusMessage.value = "Selected: ${files.first().name}"
                } else {
                    // Merge Mode: User requested multi-selection (append or overwrite logic?)
                    // The user said "if i select 1 then try to select another it just overwrites... i need to be able to select multple items"
                    // So we should APPEND if in Merge Mode? Or just let the picker handle multiples?
                    // DesktopFilePicker.pickFiles returns a list of ALL selected files from that dialog interaction.
                    // If user clicks button AGAIN, they probably expect to ADD to the list or REPLACE?
                    // "Select multiple items" usually implies either "Select All in one go" or "Add more".
                    // Let's implement ADDING for Merge mode to be safe and flexible.
                    
                    val current = _selectedFiles.value.toMutableList()
                    // Filter duplicates
                    val newFiles = files.filter { newFile -> current.none { it.path == newFile.path } }
                    current.addAll(newFiles)
                    _selectedFiles.value = current
                    
                    _statusMessage.value = "Selected ${current.size} files"
                }
            }
        }
    }
    
    fun clearSelection() {
        _selectedFiles.value = emptyList()
        _statusMessage.value = "Selection cleared"
    }

    fun changeOutputPath() {
        viewModelScope.launch {
            val dir = filePicker.pickDirectory()
            if (dir != null) {
                _outputPath.value = dir
            }
        }
    }

    fun startConversion() {
        val files = _selectedFiles.value
        if (files.isEmpty()) {
            _statusMessage.value = "Please select a file first."
            return
        }
        
        val outDir = _outputPath.value
        val isSplitting = _isSplitMode.value
        
        _isProcessing.value = true
        _statusMessage.value = if (isSplitting) "Splitting..." else "Merging..."
        
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0

            // Merge resolves the whole split-folder from a single chunk within it, so if the
            // user selected several chunks belonging to the same split set, only process one
            // representative per folder — otherwise we'd re-run the merge once per chunk and
            // write out several duplicate output files.
            val filesToProcess = if (isSplitting) {
                files
            } else {
                val seenGroups = mutableSetOf<String>()
                files.filter { file ->
                    val groupKey = if (file.isDirectory) file.path
                        else file.path.replace('\\', '/').substringBeforeLast('/', file.path)
                    seenGroups.add(groupKey)
                }
            }

            for (file in filesToProcess) {
                 _statusMessage.value = "Processing: ${file.name}..."
                 val result = if (isSplitting) {
                    fileSplitter.splitFile(file, outDir)
                } else {
                    fileSplitter.mergeFiles(file, outDir)
                }

                if (result) successCount++ else failCount++
            }
            
            _isProcessing.value = false
            if (failCount == 0) {
                _statusMessage.value = "Success! Processed $successCount files."
            } else {
                _statusMessage.value = "Done. Success: $successCount, Failed: $failCount"
            }
            println("Conversion finished. Success: $successCount, Failed: $failCount")
        }
    }
}
