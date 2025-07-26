package com.example.glaceon.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.service.FileMonitorService
import com.example.glaceon.ui.viewmodel.AutoUploadViewModel
import com.example.glaceon.util.FileUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AutoUploadSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTest: (() -> Unit)? = null,
    autoUploadViewModel: AutoUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by autoUploadViewModel.uiState.collectAsState()
    
    // Permission handling
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.FOREGROUND_SERVICE
        )
    )
    
    // Folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { autoUploadViewModel.addMonitoredFolder(it.path ?: "") }
    }
    
    LaunchedEffect(Unit) {
        autoUploadViewModel.loadSettings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Upload Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enable/Disable Auto Upload
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Upload",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Automatically upload new files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.autoUploadEnabled,
                                onCheckedChange = { enabled ->
                                    autoUploadViewModel.setAutoUploadEnabled(enabled)
                                    if (enabled) {
                                        if (permissionsState.allPermissionsGranted) {
                                            FileMonitorService.startService(context)
                                            android.util.Log.d("AutoUploadSettings", "File monitor service started")
                                        } else {
                                            permissionsState.launchMultiplePermissionRequest()
                                        }
                                    } else {
                                        FileMonitorService.stopService(context)
                                        android.util.Log.d("AutoUploadSettings", "File monitor service stopped")
                                    }
                                }
                            )
                        }
                        
                        if (!permissionsState.allPermissionsGranted && uiState.autoUploadEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Permissions required for auto upload",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = { permissionsState.launchMultiplePermissionRequest() }
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                }
            }
            
            // WiFi Only Setting
            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WiFi Only",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Upload only when connected to WiFi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.wifiOnly,
                            onCheckedChange = autoUploadViewModel::setWifiOnly
                        )
                    }
                }
            }
            
            // Auto Category Setting
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Auto Category",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Category for auto-uploaded files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = uiState.autoCategory,
                            onValueChange = autoUploadViewModel::setAutoCategory,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // File Size Limit
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "File Size Limit",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Maximum file size: ${FileUtils.formatFileSize(uiState.fileSizeLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val sizeOptions = listOf(
                            10 * 1024 * 1024L to "10 MB",
                            50 * 1024 * 1024L to "50 MB",
                            100 * 1024 * 1024L to "100 MB",
                            500 * 1024 * 1024L to "500 MB",
                            1024 * 1024 * 1024L to "1 GB"
                        )
                        
                        sizeOptions.forEach { (size, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.fileSizeLimit == size,
                                    onClick = { autoUploadViewModel.setFileSizeLimit(size) }
                                )
                                Text(text = label)
                            }
                        }
                    }
                }
            }
            
            // Monitored Folders
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monitored Folders",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { folderPickerLauncher.launch(null) }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Folder")
                            }
                        }
                        
                        Text(
                            text = "Folders to monitor for new files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
            
            // Folder List
            items(uiState.monitoredFolders.toList()) { folder ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { autoUploadViewModel.removeMonitoredFolder(folder) }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove Folder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Allowed Extensions
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Allowed File Types",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val extensionGroups = mapOf(
                            "Images" to setOf("jpg", "jpeg", "png", "gif", "bmp", "webp"),
                            "Videos" to setOf("mp4", "avi", "mov", "mkv", "webm"),
                            "Documents" to setOf("pdf", "doc", "docx", "txt", "rtf"),
                            "Archives" to setOf("zip", "rar", "7z", "tar", "gz")
                        )
                        
                        extensionGroups.forEach { (groupName, extensions) ->
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            extensions.forEach { ext ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    Checkbox(
                                        checked = ext in uiState.allowedExtensions,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                autoUploadViewModel.addAllowedExtension(ext)
                                            } else {
                                                autoUploadViewModel.removeAllowedExtension(ext)
                                            }
                                        }
                                    )
                                    Text(text = ext.uppercase())
                                }
                            }
                        }
                    }
                }
            }
            
            // Test & Debug Section
            onNavigateToTest?.let { navigateToTest ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Testing & Debug",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Test auto upload functionality",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Button(
                                onClick = navigateToTest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Test Screen")
                            }
                        }
                    }
                }
            }
        }
    }
}

