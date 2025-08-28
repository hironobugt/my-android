package com.example.glaceon.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.ui.viewmodel.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    permissionViewModel: PermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val permissionState by permissionViewModel.permissionState.collectAsState()
    
    // 権限要求ランチャー
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("PermissionScreen", "Permission result: $permissions")
        permissionViewModel.updatePermissionResult(permissions)
        
        // 必須権限が全て許可された場合は次に進む
        if (permissionState.hasRequiredPermissions) {
            android.util.Log.d("PermissionScreen", "All required permissions granted, proceeding...")
            onPermissionsGranted()
        } else {
            android.util.Log.d("PermissionScreen", "Some required permissions still denied")
        }
    }
    
    // 初回権限チェック
    LaunchedEffect(Unit) {
        android.util.Log.d("PermissionScreen", "Starting permission check with context: ${context::class.java.simpleName}")
        permissionViewModel.checkPermissions(context)
    }
    
    // 権限が既に許可されている場合は自動で次に進む
    LaunchedEffect(permissionState.hasRequiredPermissions, permissionState.hasOptionalPermissions, permissionState.permissionCheckComplete) {
        // 必須権限のみで判定（推奨）
        if (permissionState.hasRequiredPermissions && permissionState.permissionCheckComplete) {
            onPermissionsGranted()
        }
        
        // すべての権限で判定したい場合は以下をコメントアウト
        // if (permissionState.hasRequiredPermissions && permissionState.hasOptionalPermissions && permissionState.permissionCheckComplete) {
        //     onPermissionsGranted()
        // }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // アイコンとタイトル
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "権限の許可",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Glacier Archiveが正常に動作するために、以下の権限が必要です。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 権限リスト
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 必須権限
            item {
                Text(
                    text = "必須権限",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(permissionViewModel.getRequiredPermissions()) { permission ->
                PermissionItem(
                    permission = permission,
                    description = permissionViewModel.getPermissionDescription(permission),
                    isRequired = true,
                    isGranted = permission !in permissionState.deniedPermissions
                )
            }
            
            // オプション権限
            if (permissionViewModel.getOptionalPermissions().isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "オプション権限",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "これらの権限は必須ではありませんが、より多くの機能を利用できます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                items(permissionViewModel.getOptionalPermissions()) { permission ->
                    PermissionItem(
                        permission = permission,
                        description = permissionViewModel.getPermissionDescription(permission),
                        isRequired = false,
                        isGranted = permission !in permissionState.deniedPermissions
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ボタン
        if (permissionState.permissionCheckComplete) {
            if (!permissionState.hasRequiredPermissions) {
                Button(
                    onClick = {
                        // 必須権限とオプション権限の両方をリクエスト
                        val permissionsToRequest = permissionViewModel.getAllPermissions().toTypedArray()
                        android.util.Log.d("PermissionScreen", "=== Permission Request Start ===")
                        android.util.Log.d("PermissionScreen", "Requesting REQUIRED permissions only: ${permissionsToRequest.contentToString()}")
                        android.util.Log.d("PermissionScreen", "Permission state: hasRequired=${permissionState.hasRequiredPermissions}, denied=${permissionState.deniedPermissions}")
                        android.util.Log.d("PermissionScreen", "Should show rationale: ${permissionState.shouldShowRationale}")
                        
                        try {
                            permissionLauncher.launch(permissionsToRequest)
                            android.util.Log.d("PermissionScreen", "Permission launcher executed successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("PermissionScreen", "Failed to launch permission request", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("権限を許可する")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        // 設定画面を開く
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("設定画面で許可する")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // デバッグ用：個別権限テスト
                OutlinedButton(
                    onClick = {
                        // 写真権限のみをテスト
                        android.util.Log.d("PermissionScreen", "Testing READ_MEDIA_IMAGES permission only")
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("写真権限のみテスト")
                }
            } else {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("続行")
                }
            }
        } else {
            CircularProgressIndicator()
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionItem(
    permission: String,
    description: String,
    isRequired: Boolean,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (isRequired) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isGranted -> Icons.Default.CheckCircle
                    isRequired -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when {
                    isGranted -> MaterialTheme.colorScheme.onPrimaryContainer
                    isRequired -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getPermissionName(permission),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isRequired) {
                    Text(
                        text = "必須",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "許可済み",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun getPermissionName(permission: String): String {
    return when (permission) {
        android.Manifest.permission.READ_MEDIA_IMAGES -> "写真の読み取り"
        android.Manifest.permission.READ_MEDIA_VIDEO -> "動画の読み取り"
        android.Manifest.permission.READ_MEDIA_AUDIO -> "音声の読み取り"
        android.Manifest.permission.READ_EXTERNAL_STORAGE -> "ストレージの読み取り"
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "すべてのファイルアクセス"
        android.Manifest.permission.POST_NOTIFICATIONS -> "通知の表示"
        else -> "不明な権限"
    }
}