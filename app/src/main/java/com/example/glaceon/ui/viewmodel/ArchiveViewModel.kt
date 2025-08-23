package com.example.glaceon.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glaceon.data.model.ArchiveItem
import com.example.glaceon.data.model.ArchiveStatus
import com.example.glaceon.data.repository.ArchiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    
    private val archiveRepository = ArchiveRepository(application)
    
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()
    
    private val _archives = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val archives: StateFlow<List<ArchiveItem>> = _archives.asStateFlow()
    
    // サムネイルURLキャッシュ
    private val _thumbnailUrlCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val thumbnailUrlCache: StateFlow<Map<String, String>> = _thumbnailUrlCache.asStateFlow()
    
    fun loadArchives(token: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            archiveRepository.getArchiveList(token).fold(
                onSuccess = { response ->
                    _archives.value = response.archives
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasMore = response.hasMore,
                        continuationToken = response.continuationToken
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun loadMoreArchives(token: String) {
        val currentState = _uiState.value
        
        // 既にロード中、または追加データがない場合は何もしない
        if (currentState.isLoading || !currentState.hasMore || currentState.continuationToken == null) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
            
            archiveRepository.getArchiveList(
                token = token,
                continuationToken = currentState.continuationToken
            ).fold(
                onSuccess = { response ->
                    // 既存のリストに新しいアイテムを追加
                    _archives.value = _archives.value + response.archives
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasMore = response.hasMore,
                        continuationToken = response.continuationToken
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun uploadFile(
        token: String,
        fileUri: Uri,
        fileName: String,
        description: String? = null,
        category: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            
            val metadata = mutableMapOf<String, String>().apply {
                description?.let { put("description", it) }
                category?.let { put("category", it) }
                put("uploadedFrom", "android-app")
            }
            
            archiveRepository.uploadFile(token, fileUri, fileName, metadata).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        message = "File uploaded successfully: ${response.archiveId}"
                    )
                    
                    // Add the uploaded file to the archive list immediately
                    val newArchiveItem = ArchiveItem(
                        archiveId = response.archiveId,
                        fileName = fileName,
                        fileSize = try {
                            val inputStream = getApplication<Application>().contentResolver.openInputStream(fileUri)
                            val size = inputStream?.available()?.toLong() ?: 0L
                            inputStream?.close()
                            size
                        } catch (e: Exception) {
                            0L
                        },
                        uploadTimestamp = kotlinx.datetime.Clock.System.now().toString(),
                        status = "ARCHIVED",
                        metadata = metadata,
                        description = description,
                        category = category
                    )
                    
                    // Add to the beginning of the list
                    _archives.value = listOf(newArchiveItem) + _archives.value
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun restoreArchive(token: String, archiveId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            archiveRepository.restoreArchive(token, archiveId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = when (response.status) {
                            "restored" -> if (response.content != null) {
                                "File restored and saved to Downloads folder: ${response.fileName}"
                            } else {
                                "File is ready for download"
                            }
                            "restoring" -> "Restore in progress. Please check back later."
                            "restore_initiated" -> "Restore request initiated. Please check back in 12-48 hours."
                            else -> response.message
                        }
                    )
                    
                    // Update the specific archive status
                    _archives.value = _archives.value.map { archive ->
                        if (archive.archiveId == archiveId) {
                            archive.copy(
                                status = when (response.status) {
                                    "restored" -> "RESTORED"
                                    "restoring" -> "RESTORING"
                                    "restore_initiated" -> "RESTORING"
                                    else -> archive.status
                                }
                            )
                        } else {
                            archive
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun deleteArchive(token: String, archiveId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            archiveRepository.deleteArchive(token, archiveId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Archive deleted successfully"
                    )
                    
                    // Remove the archive from the list
                    _archives.value = _archives.value.filter { it.archiveId != archiveId }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun loadThumbnail(token: String, archiveId: String) {
        // キャッシュにある場合は何もしない
        if (_thumbnailUrlCache.value.containsKey(archiveId)) {
            Log.d("ArchiveViewModel", "Thumbnail URL already cached for $archiveId")
            return
        }
        
        Log.d("ArchiveViewModel", "Loading thumbnail URL for $archiveId")
        viewModelScope.launch {
            archiveRepository.getThumbnail(token, archiveId).fold(
                onSuccess = { thumbnailResponse ->
                    Log.d("ArchiveViewModel", "Thumbnail URL received for $archiveId: ${thumbnailResponse.thumbnailUrl}")
                    _thumbnailUrlCache.value = _thumbnailUrlCache.value + (archiveId to thumbnailResponse.thumbnailUrl)
                },
                onFailure = { error ->
                    Log.e("ArchiveViewModel", "Failed to load thumbnail URL for $archiveId: ${error.message}")
                }
            )
        }
    }
    
    fun getThumbnailUrl(archiveId: String): String? {
        return _thumbnailUrlCache.value[archiveId]
    }
    
    fun downloadFile(token: String, archiveId: String, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            archiveRepository.downloadFile(token, archiveId).fold(
                onSuccess = { downloadResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = when (downloadResponse.status) {
                            "success" -> "File downloaded successfully: $fileName"
                            "ready" -> "File is ready for download"
                            "not_ready" -> "File is not ready for download yet. Please wait for restoration to complete."
                            else -> downloadResponse.message ?: "Download completed"
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Download failed: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun checkRestoreStatus(token: String, archiveId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            archiveRepository.getArchiveStatus(token, archiveId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = when (response.status) {
                            "restored" -> "File is ready for download!"
                            "restoring" -> "Restore still in progress. Please check back later."
                            "restore_initiated" -> "Restore request is being processed."
                            else -> response.message
                        }
                    )
                    
                    // Update the specific archive status
                    _archives.value = _archives.value.map { archive ->
                        if (archive.archiveId == archiveId) {
                            archive.copy(
                                status = when (response.status) {
                                    "restored" -> "RESTORED"
                                    "restoring" -> "RESTORING"
                                    "restore_initiated" -> "RESTORING"
                                    else -> archive.status
                                }
                            )
                        } else {
                            archive
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to check restore status: ${error.message}"
                    )
                }
            )
        }
    }
}

data class ArchiveUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isUploading: Boolean = false,
    val hasMore: Boolean = false,
    val continuationToken: String? = null,
    val error: String? = null,
    val message: String? = null
)