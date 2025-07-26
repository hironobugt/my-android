package com.example.glaceon.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auto_upload_settings")

class AutoUploadPreferences(private val context: Context) {
    
    companion object {
        private val AUTO_UPLOAD_ENABLED = booleanPreferencesKey("auto_upload_enabled")
        private val MONITORED_FOLDERS = stringSetPreferencesKey("monitored_folders")
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val AUTO_CATEGORY = stringPreferencesKey("auto_category")
        private val FILE_SIZE_LIMIT = longPreferencesKey("file_size_limit")
        private val ALLOWED_EXTENSIONS = stringSetPreferencesKey("allowed_extensions")
    }
    
    val autoUploadEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_UPLOAD_ENABLED] ?: false
    }
    
    val monitoredFolders: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[MONITORED_FOLDERS] ?: run {
            val defaultFolders = mutableSetOf<String>()
            
            // Standard folders
            defaultFolders.add("/storage/emulated/0/Download")
            defaultFolders.add("/storage/emulated/0/DCIM/Camera")
            defaultFolders.add("/storage/emulated/0/Pictures")
            
            // Screenshot folders - try multiple common locations
            val screenshotPaths = listOf(
                "/storage/emulated/0/Pictures/Screenshots",
                "/storage/emulated/0/DCIM/Screenshots",
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath + "/Screenshots",
                android.os.Environment.getExternalStorageDirectory().absolutePath + "/Pictures/Screenshots",
                android.os.Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Screenshots"
            )
            
            // Add screenshot folders that actually exist
            screenshotPaths.forEach { path ->
                if (java.io.File(path).exists()) {
                    defaultFolders.add(path)
                }
            }
            
            defaultFolders
        }
    }
    
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WIFI_ONLY] ?: true
    }
    
    val autoCategory: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CATEGORY] ?: "auto-upload"
    }
    
    val fileSizeLimit: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[FILE_SIZE_LIMIT] ?: (100 * 1024 * 1024L) // 100MB default
    }
    
    val allowedExtensions: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[ALLOWED_EXTENSIONS] ?: setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", // Images
            "mp4", "avi", "mov", "mkv", "webm", // Videos
            "pdf", "doc", "docx", "txt", "rtf", // Documents
            "zip", "rar", "7z", "tar", "gz" // Archives
        )
    }
    
    suspend fun setAutoUploadEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPLOAD_ENABLED] = enabled
        }
    }
    
    suspend fun addMonitoredFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFolders = preferences[MONITORED_FOLDERS] ?: emptySet()
            preferences[MONITORED_FOLDERS] = currentFolders + folderPath
        }
    }
    
    suspend fun removeMonitoredFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFolders = preferences[MONITORED_FOLDERS] ?: emptySet()
            preferences[MONITORED_FOLDERS] = currentFolders - folderPath
        }
    }
    
    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY] = wifiOnly
        }
    }
    
    suspend fun setAutoCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CATEGORY] = category
        }
    }
    
    suspend fun setFileSizeLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[FILE_SIZE_LIMIT] = limitBytes
        }
    }
    
    suspend fun setAllowedExtensions(extensions: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[ALLOWED_EXTENSIONS] = extensions
        }
    }
}