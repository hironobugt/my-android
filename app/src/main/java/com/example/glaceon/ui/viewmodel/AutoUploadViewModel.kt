package com.example.glaceon.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glaceon.data.preferences.AutoUploadPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AutoUploadViewModel(application: Application) : AndroidViewModel(application) {
    
    private val autoUploadPreferences = AutoUploadPreferences(application)
    
    private val _uiState = MutableStateFlow(AutoUploadUiState())
    val uiState: StateFlow<AutoUploadUiState> = _uiState.asStateFlow()
    
    fun loadSettings() {
        viewModelScope.launch {
            combine(
                autoUploadPreferences.autoUploadEnabled,
                autoUploadPreferences.monitoredFolders,
                autoUploadPreferences.wifiOnly,
                autoUploadPreferences.autoCategory,
                autoUploadPreferences.fileSizeLimit,
                autoUploadPreferences.allowedExtensions
            ) { values ->
                val enabled = values[0] as? Boolean ?: false
                val folders = values[1] as? Set<*> ?: emptySet<String>()
                val wifiOnly = values[2] as? Boolean ?: true
                val category = values[3] as? String ?: "auto-upload"
                val sizeLimit = values[4] as? Long ?: 100 * 1024 * 1024L
                val extensions = values[5] as? Set<*> ?: emptySet<String>()
                
                AutoUploadUiState(
                    autoUploadEnabled = enabled,
                    monitoredFolders = folders.filterIsInstance<String>().toSet(),
                    wifiOnly = wifiOnly,
                    autoCategory = category,
                    fileSizeLimit = sizeLimit,
                    allowedExtensions = extensions.filterIsInstance<String>().toSet()
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun setAutoUploadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            autoUploadPreferences.setAutoUploadEnabled(enabled)
        }
    }
    
    fun addMonitoredFolder(folderPath: String) {
        viewModelScope.launch {
            autoUploadPreferences.addMonitoredFolder(folderPath)
        }
    }
    
    fun removeMonitoredFolder(folderPath: String) {
        viewModelScope.launch {
            autoUploadPreferences.removeMonitoredFolder(folderPath)
        }
    }
    
    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            autoUploadPreferences.setWifiOnly(wifiOnly)
        }
    }
    
    fun setAutoCategory(category: String) {
        viewModelScope.launch {
            autoUploadPreferences.setAutoCategory(category)
        }
    }
    
    fun setFileSizeLimit(limitBytes: Long) {
        viewModelScope.launch {
            autoUploadPreferences.setFileSizeLimit(limitBytes)
        }
    }
    
    fun addAllowedExtension(extension: String) {
        viewModelScope.launch {
            val currentExtensions = _uiState.value.allowedExtensions
            autoUploadPreferences.setAllowedExtensions(currentExtensions + extension)
        }
    }
    
    fun removeAllowedExtension(extension: String) {
        viewModelScope.launch {
            val currentExtensions = _uiState.value.allowedExtensions
            autoUploadPreferences.setAllowedExtensions(currentExtensions - extension)
        }
    }
}

data class AutoUploadUiState(
    val autoUploadEnabled: Boolean = false,
    val monitoredFolders: Set<String> = emptySet(),
    val wifiOnly: Boolean = true,
    val autoCategory: String = "auto-upload",
    val fileSizeLimit: Long = 100 * 1024 * 1024L, // 100MB
    val allowedExtensions: Set<String> = emptySet()
)