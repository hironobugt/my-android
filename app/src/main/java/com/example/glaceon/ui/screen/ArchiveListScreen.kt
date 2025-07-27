package com.example.glaceon.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.data.model.ArchiveItem
import com.example.glaceon.data.model.ArchiveStatus
import com.example.glaceon.ui.viewmodel.ArchiveViewModel
import com.example.glaceon.ui.viewmodel.AuthViewModel
import com.example.glaceon.util.FileUtils
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveListScreen(
        onNavigateToSettings: () -> Unit,
        onNavigateToApiTest: () -> Unit,
        onNavigateToBilling: () -> Unit,
        onSignOut: () -> Unit,
        authViewModel: AuthViewModel = viewModel(),
        archiveViewModel: ArchiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by archiveViewModel.uiState.collectAsState()
    val archives by archiveViewModel.archives.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher
    val filePickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri
                ->
                uri?.let {
                    selectedFileUri = it
                    showUploadDialog = true
                }
            }

    // Load archives when screen loads
    LaunchedEffect(Unit) {
        authViewModel.getAccessToken()?.let { token -> archiveViewModel.loadArchives(token) }
    }

    // Auto-start file monitoring service if auto upload is enabled
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Check if auto upload is enabled and start service
            val autoUploadPreferences =
                    com.example.glaceon.data.preferences.AutoUploadPreferences(context)
            if (autoUploadPreferences.autoUploadEnabled.first()) {
                com.example.glaceon.service.FileMonitorService.startService(context)
            }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Glaceon Archive") },
                        actions = {
                            IconButton(onClick = onNavigateToApiTest) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "API Test")
                            }
                            IconButton(onClick = onNavigateToBilling) {
                                Icon(Icons.Default.Payment, contentDescription = "Billing")
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Auto Upload Settings"
                                )
                            }
                            IconButton(onClick = onSignOut) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sign Out"
                                )
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.Add, contentDescription = "Upload File")
                }
            }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = {
                authViewModel.getAccessToken()?.let { token ->
                    archiveViewModel.loadArchives(token, refresh = true)
                }
            },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // User info
            currentUser?.let { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Welcome, ${user.username}",
                                style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            // Archive list with infinite scroll
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archives) { archive ->
                    ArchiveItemCard(
                            archive = archive,
                            archiveViewModel = archiveViewModel,
                            authViewModel = authViewModel,
                            onRestore = { archiveId: String ->
                                authViewModel.getAccessToken()?.let { token ->
                                    archiveViewModel.restoreArchive(token, archiveId)
                                }
                            },
                            onDelete = { archiveId: String ->
                                authViewModel.getAccessToken()?.let { token ->
                                    archiveViewModel.deleteArchive(token, archiveId)
                                }
                            }
                    )
                }
                
                // Load more indicator and trigger
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(Unit) {
                            // Trigger load more when this item becomes visible
                            authViewModel.getAccessToken()?.let { token ->
                                archiveViewModel.loadMoreArchives(token)
                            }
                        }
                        
                        if (uiState.isLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                if (archives.isEmpty() && !uiState.isLoading) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                        Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                        text = "No archives yet",
                                        style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                        text = "Tap the + button to upload your first file",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        } // PullToRefreshBox closing brace
    } // Scaffold closing brace

    // Upload dialog
    if (showUploadDialog && selectedFileUri != null) {
        UploadDialog(
                fileUri = selectedFileUri!!,
                onDismiss = {
                    showUploadDialog = false
                    selectedFileUri = null
                },
                onUpload = { fileName: String, description: String, category: String ->
                    authViewModel.getAccessToken()?.let { token ->
                        archiveViewModel.uploadFile(
                                token = token,
                                fileUri = selectedFileUri!!,
                                fileName = fileName,
                                description = description.takeIf { it.isNotBlank() },
                                category = category.takeIf { it.isNotBlank() }
                        )
                    }
                    // Reset dialog state
                    showUploadDialog = false
                    selectedFileUri = null
                }
        )
    }

    // Error/Message snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            archiveViewModel.clearError()
        }
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or handle message
            archiveViewModel.clearMessage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveItemCard(
        archive: ArchiveItem,
        archiveViewModel: ArchiveViewModel,
        authViewModel: AuthViewModel,
        onRestore: (String) -> Unit,
        onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val thumbnailCache by archiveViewModel.thumbnailCache.collectAsState()

    // サムネイルがある場合は読み込む
    LaunchedEffect(archive.archiveId, archive.hasThumbnail) {
        if (archive.hasThumbnail && !thumbnailCache.containsKey(archive.archiveId)) {
            authViewModel.getAccessToken()?.let { token ->
                archiveViewModel.loadThumbnail(token, archive.archiveId)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
            ) {
                // サムネイル表示
                Box(
                        modifier =
                                Modifier.size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                ) {
                    val thumbnail = thumbnailCache[archive.archiveId]
                    if (thumbnail != null) {
                        Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = "Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                    } else {
                        // ファイルタイプに応じたアイコンを表示
                        val icon =
                                when (archive.fileType) {
                                    "image" -> Icons.Default.Image
                                    "video" -> Icons.Default.VideoFile
                                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                }
                        Icon(
                                imageVector = icon,
                                contentDescription = "File type",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ファイル情報
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = archive.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )

                    Text(
                            text = FileUtils.formatFileSize(archive.fileSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                            text =
                                    try {
                                        archive.uploadDate
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                                .toString()
                                    } catch (e: Exception) {
                                        "Upload date unavailable"
                                    },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    archive.description?.let { description ->
                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                StatusChip(status = archive.archiveStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (archive.archiveStatus == ArchiveStatus.ARCHIVED) {
                    OutlinedButton(onClick = { onRestore(archive.archiveId) }) {
                        Icon(
                                Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                }

                OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors =
                                ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                )
                ) {
                    Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Archive") },
                text = {
                    Text(
                            "Are you sure you want to delete '${archive.fileName}'? This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                onDelete(archive.archiveId)
                                showDeleteDialog = false
                            }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@Composable
fun StatusChip(status: ArchiveStatus?) {
    val (text, color) =
            when (status) {
                ArchiveStatus.UPLOADING -> "Uploading" to MaterialTheme.colorScheme.primary
                ArchiveStatus.ARCHIVED -> "Archived" to MaterialTheme.colorScheme.secondary
                ArchiveStatus.RESTORING -> "Restoring" to MaterialTheme.colorScheme.tertiary
                ArchiveStatus.RESTORED -> "Restored" to MaterialTheme.colorScheme.primary
                ArchiveStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
                null -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
            }

    AssistChip(
            onClick = {},
            label = { Text(text) },
            colors = AssistChipDefaults.assistChipColors(labelColor = color)
    )
}

