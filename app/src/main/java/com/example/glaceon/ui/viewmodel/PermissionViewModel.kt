package com.example.glaceon.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionViewModel : ViewModel() {
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    data class PermissionState(
        val isLoading: Boolean = false,
        val hasRequiredPermissions: Boolean = false,
        val hasOptionalPermissions: Boolean = false,
        val deniedPermissions: List<String> = emptyList(),
        val shouldShowRationale: Map<String, Boolean> = emptyMap(),
        val permissionCheckComplete: Boolean = false
    )
    
    // 必須権限（自動アップロード用）
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    
    // オプション権限
    private val optionalPermissions = mutableListOf<String>().apply {
        // 音声ファイル
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        
        // 通知権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // すべてのファイルアクセス（Android 11+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }
    }
    
    fun checkPermissions(context: Context) {
        viewModelScope.launch {
            _permissionState.value = _permissionState.value.copy(isLoading = true)
            
            android.util.Log.d("PermissionViewModel", "=== Permission Check Start ===")
            android.util.Log.d("PermissionViewModel", "Android SDK: ${Build.VERSION.SDK_INT}")
            android.util.Log.d("PermissionViewModel", "Required permissions: $requiredPermissions")
            android.util.Log.d("PermissionViewModel", "Optional permissions: $optionalPermissions")
            
            // 権限の詳細状態をチェック
            val rationaleMap = mutableMapOf<String, Boolean>()
            
            val deniedRequired = requiredPermissions.filter { permission ->
                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                val shouldShowRationale = if (context is android.app.Activity) {
                    ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
                } else false
                
                rationaleMap[permission] = shouldShowRationale
                
                android.util.Log.d("PermissionViewModel", "Required permission $permission:")
                android.util.Log.d("PermissionViewModel", "  - Granted: $granted")
                android.util.Log.d("PermissionViewModel", "  - Should show rationale: $shouldShowRationale")
                
                !granted
            }
            
            val deniedOptional = optionalPermissions.filter { permission ->
                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                val shouldShowRationale = if (context is android.app.Activity) {
                    ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
                } else false
                
                rationaleMap[permission] = shouldShowRationale
                
                android.util.Log.d("PermissionViewModel", "Optional permission $permission:")
                android.util.Log.d("PermissionViewModel", "  - Granted: $granted")
                android.util.Log.d("PermissionViewModel", "  - Should show rationale: $shouldShowRationale")
                
                !granted
            }
            
            android.util.Log.d("PermissionViewModel", "=== Permission Check Results ===")
            android.util.Log.d("PermissionViewModel", "Denied required: $deniedRequired")
            android.util.Log.d("PermissionViewModel", "Denied optional: $deniedOptional")
            android.util.Log.d("PermissionViewModel", "Has required permissions: ${deniedRequired.isEmpty()}")
            android.util.Log.d("PermissionViewModel", "Rationale map: $rationaleMap")
            android.util.Log.d("PermissionViewModel", "=== Permission Check End ===")
            
            _permissionState.value = _permissionState.value.copy(
                isLoading = false,
                hasRequiredPermissions = deniedRequired.isEmpty(),
                hasOptionalPermissions = deniedOptional.isEmpty(),
                deniedPermissions = deniedRequired + deniedOptional,
                shouldShowRationale = rationaleMap,
                permissionCheckComplete = true
            )
        }
    }
    
    fun updatePermissionResult(permissions: Map<String, Boolean>) {
        val deniedRequired = requiredPermissions.filter { permission ->
            permissions[permission] == false
        }
        
        val deniedOptional = optionalPermissions.filter { permission ->
            permissions[permission] == false
        }
        
        _permissionState.value = _permissionState.value.copy(
            hasRequiredPermissions = deniedRequired.isEmpty(),
            hasOptionalPermissions = deniedOptional.isEmpty(),
            deniedPermissions = deniedRequired + deniedOptional
        )
    }
    
    fun getRequiredPermissions(): List<String> = requiredPermissions
    fun getOptionalPermissions(): List<String> = optionalPermissions
    fun getAllPermissions(): List<String> = requiredPermissions + optionalPermissions
    
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_MEDIA_IMAGES -> "写真の自動アップロードに必要です"
            Manifest.permission.READ_MEDIA_VIDEO -> "動画の自動アップロードに必要です"
            Manifest.permission.READ_MEDIA_AUDIO -> "音声ファイルのアーカイブに使用します"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "ファイルの読み取りに必要です"
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "すべてのファイル形式のアーカイブに使用します"
            Manifest.permission.POST_NOTIFICATIONS -> "復元完了などの通知に使用します"
            else -> "アプリの機能に必要な権限です"
        }
    }
    
    fun isRequiredPermission(permission: String): Boolean {
        return permission in requiredPermissions
    }
}