package com.example.glaceon.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat

import com.example.glaceon.R
import com.example.glaceon.data.preferences.AutoUploadPreferences
import com.example.glaceon.data.repository.ArchiveRepository
import com.example.glaceon.data.repository.AuthRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class FileMonitorService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fileObservers = mutableListOf<FileObserver>()
    private lateinit var autoUploadPreferences: AutoUploadPreferences
    private lateinit var archiveRepository: ArchiveRepository
    private lateinit var authRepository: AuthRepository
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "file_monitor_channel"
        
        fun startService(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        autoUploadPreferences = AutoUploadPreferences(this)
        archiveRepository = ArchiveRepository(this)
        authRepository = AuthRepository(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("File monitoring active"))
        
        serviceScope.launch {
            startMonitoring()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
    
    private suspend fun startMonitoring() {
        val monitoredFolders = autoUploadPreferences.monitoredFolders.first()
        
        monitoredFolders.forEach { folderPath ->
            val folder = File(folderPath)
            if (folder.exists() && folder.isDirectory) {
                android.util.Log.d("FileMonitorService", "Starting to monitor folder: $folderPath")
                val observer = createFileObserver(folderPath)
                observer.startWatching()
                fileObservers.add(observer)
            } else {
                android.util.Log.w("FileMonitorService", "Folder does not exist or is not a directory: $folderPath")
            }
        }
        
        // Start periodic scanning as backup
        startPeriodicScanning()
    }
    
    private fun startPeriodicScanning() {
        serviceScope.launch {
            val knownFiles = mutableSetOf<String>()
            
            // Initial scan to populate known files
            try {
                val monitoredFolders = autoUploadPreferences.monitoredFolders.first()
                android.util.Log.d("FileMonitorService", "Initial scan of monitored folders: $monitoredFolders")
                
                monitoredFolders.forEach { folderPath ->
                    val folder = File(folderPath)
                    if (folder.exists() && folder.isDirectory) {
                        android.util.Log.d("FileMonitorService", "✅ Scanning existing folder: $folderPath")
                        folder.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                knownFiles.add(file.absolutePath)
                                android.util.Log.d("FileMonitorService", "Known file: ${file.name} (${file.length()} bytes)")
                            }
                        }
                    } else {
                        android.util.Log.w("FileMonitorService", "❌ Folder does not exist: $folderPath")
                    }
                }
                
                android.util.Log.i("FileMonitorService", "📊 Initial scan complete. Known files: ${knownFiles.size}")
            } catch (e: Exception) {
                android.util.Log.e("FileMonitorService", "Error in initial scan: ${e.message}")
            }
            
            while (true) {
                try {
                    val monitoredFolders = autoUploadPreferences.monitoredFolders.first()
                    var newFilesFound = 0
                    
                    monitoredFolders.forEach { folderPath ->
                        val folder = File(folderPath)
                        if (folder.exists() && folder.isDirectory) {
                            folder.listFiles()?.forEach { file ->
                                if (file.isFile && !knownFiles.contains(file.absolutePath)) {
                                    knownFiles.add(file.absolutePath)
                                    newFilesFound++
                                    
                                    android.util.Log.i("FileMonitorService", "🆕 NEW FILE FOUND: ${file.name}")
                                    android.util.Log.d("FileMonitorService", "   Path: ${file.absolutePath}")
                                    android.util.Log.d("FileMonitorService", "   Size: ${file.length()} bytes")
                                    android.util.Log.d("FileMonitorService", "   Modified: ${java.util.Date(file.lastModified())}")
                                    
                                    // Special logging for screenshots
                                    if (file.name.contains("Screenshot", ignoreCase = true) || 
                                        file.name.contains("screen", ignoreCase = true)) {
                                        android.util.Log.i("FileMonitorService", "📸 SCREENSHOT DETECTED: ${file.name}")
                                        showNotification("Screenshot detected: ${file.name}")
                                    }
                                    
                                    handleNewFile(file.absolutePath)
                                }
                            }
                        }
                    }
                    
                    if (newFilesFound > 0) {
                        android.util.Log.i("FileMonitorService", "📊 Scan complete. New files found: $newFilesFound")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FileMonitorService", "Error in periodic scanning: ${e.message}")
                }
                
                // Scan every 5 seconds for better responsiveness
                delay(5000)
            }
        }
    }
    
    private fun stopMonitoring() {
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()
    }
    
    private fun createFileObserver(folderPath: String): FileObserver {
        android.util.Log.d("FileMonitorService", "Creating FileObserver for: $folderPath")
        
        return object : FileObserver(File(folderPath)) {
            override fun onEvent(event: Int, path: String?) {
                val eventName = when (event) {
                    CREATE -> "CREATE"
                    DELETE -> "DELETE"
                    MODIFY -> "MODIFY"
                    MOVED_FROM -> "MOVED_FROM"
                    MOVED_TO -> "MOVED_TO"
                    CLOSE_WRITE -> "CLOSE_WRITE"
                    else -> "OTHER($event)"
                }
                android.util.Log.d("FileMonitorService", "FileObserver event: $eventName for file: $path in $folderPath")
                
                if (event and (CREATE or MOVED_TO or CLOSE_WRITE) != 0) {
                    path?.let { fileName ->
                        val fullPath = "$folderPath/$fileName"
                        android.util.Log.d("FileMonitorService", "Processing file event: $fullPath")
                        
                        // Special handling for screenshots
                        if (fileName.contains("Screenshot", ignoreCase = true) || 
                            fileName.contains("screen", ignoreCase = true)) {
                            android.util.Log.i("FileMonitorService", "🔍 SCREENSHOT EVENT: $eventName for $fileName")
                        }
                        
                        serviceScope.launch {
                            handleNewFile(fullPath)
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun handleNewFile(filePath: String) {
        try {
            android.util.Log.d("FileMonitorService", "New file detected: $filePath")
            val file = File(filePath)
            
            // Skip temporary/pending files
            if (file.name.startsWith(".pending-") || file.name.startsWith(".tmp") || file.name.startsWith("~")) {
                android.util.Log.d("FileMonitorService", "Skipping temporary file: ${file.name}")
                return
            }
            
            // Check if file exists and is readable
            if (!file.exists() || !file.canRead() || file.isDirectory) {
                android.util.Log.d("FileMonitorService", "File check failed: exists=${file.exists()}, canRead=${file.canRead()}, isDirectory=${file.isDirectory}")
                return
            }
            
            // Wait a bit to ensure file is completely written
            android.util.Log.d("FileMonitorService", "Waiting for file to be completely written...")
            delay(2000)
            
            // Check file again after delay
            if (!file.exists() || file.length() == 0L) {
                android.util.Log.d("FileMonitorService", "File disappeared or empty after delay: exists=${file.exists()}, size=${file.length()}")
                return
            }
            
            // Check if auto upload is enabled
            val autoUploadEnabled = autoUploadPreferences.autoUploadEnabled.first()
            android.util.Log.d("FileMonitorService", "Auto upload enabled: $autoUploadEnabled")
            if (!autoUploadEnabled) {
                return
            }
            
            // Check file extension
            val allowedExtensions = autoUploadPreferences.allowedExtensions.first()
            val fileExtension = file.extension.lowercase()
            android.util.Log.d("FileMonitorService", "File extension: $fileExtension, allowed: $allowedExtensions")
            if (fileExtension !in allowedExtensions) {
                android.util.Log.d("FileMonitorService", "File extension not allowed: $fileExtension")
                return
            }
            
            // Check file size limit
            val fileSizeLimit = autoUploadPreferences.fileSizeLimit.first()
            android.util.Log.d("FileMonitorService", "File size: ${file.length()}, limit: $fileSizeLimit")
            if (file.length() > fileSizeLimit) {
                showNotification("File too large: ${file.name}")
                return
            }
            
            // Check network conditions
            val wifiOnly = autoUploadPreferences.wifiOnly.first()
            val isWifi = isWifiConnected()
            android.util.Log.d("FileMonitorService", "WiFi only: $wifiOnly, is WiFi: $isWifi")
            if (wifiOnly && !isWifi) {
                showNotification("Waiting for WiFi: ${file.name}")
                return
            }
            
            // Check authentication
            val token = authRepository.getAccessToken()
            android.util.Log.d("FileMonitorService", "Auth token available: ${token != null}")
            if (token == null) {
                showNotification("Authentication required for auto-upload")
                return
            }
            
            // Upload file
            android.util.Log.d("FileMonitorService", "Starting upload for: ${file.name}")
            uploadFile(file, token)
            
        } catch (e: Exception) {
            android.util.Log.e("FileMonitorService", "Error processing file: ${e.message}", e)
            showNotification("Error processing file: ${e.message}")
        }
    }
    
    private suspend fun uploadFile(file: File, token: String) {
        try {
            showNotification("Uploading: ${file.name}")
            
            val category = autoUploadPreferences.autoCategory.first()
            val metadata = mapOf(
                "description" to "Auto-uploaded from ${file.parent}",
                "category" to category,
                "originalPath" to file.absolutePath
            )
            
            val result = archiveRepository.uploadFile(
                token = token,
                fileUri = android.net.Uri.fromFile(file),
                fileName = file.name,
                metadata = metadata
            )
            
            result.fold(
                onSuccess = { response ->
                    showNotification("✓ Uploaded: ${file.name}")
                    // Optionally delete the original file after successful upload
                    // file.delete()
                },
                onFailure = { error ->
                    showNotification("✗ Upload failed: ${file.name} - ${error.message}")
                }
            )
            
        } catch (e: Exception) {
            showNotification("✗ Upload error: ${file.name} - ${e.message}")
        }
    }
    
    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors files for automatic upload"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Glaceon Auto Upload")
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    
    private fun showNotification(message: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }
}