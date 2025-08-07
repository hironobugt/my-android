package com.example.glaceon.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.glaceon.data.api.ApiClient
import com.example.glaceon.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class ArchiveRepository(private val context: Context) {
    
    private val api = ApiClient.glaceonApi
    
    suspend fun uploadFile(
        token: String,
        fileUri: Uri,
        fileName: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Check file size first
            val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Cannot access file"))
            }
            
            // Read file with size limit (10MB max for safety)
            val maxSize = 10 * 1024 * 1024 // 10MB
            val fileBytes = try {
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                if (bytes.size > maxSize) {
                    return@withContext Result.failure(Exception("File too large (max 10MB)"))
                }
                bytes
            } catch (e: OutOfMemoryError) {
                inputStream.close()
                return@withContext Result.failure(Exception("File too large to process"))
            } catch (e: Exception) {
                inputStream.close()
                return@withContext Result.failure(Exception("Failed to read file: ${e.message}"))
            }
            
            // Convert to Base64
            val base64Content = try {
                Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            } catch (e: OutOfMemoryError) {
                return@withContext Result.failure(Exception("File too large for Base64 encoding"))
            }
            
            // Create request
            val request = UploadRequest(
                fileName = fileName,
                fileContent = base64Content,
                metadata = metadata
            )
            
            // Make API call
            val response = try {
                val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                api.uploadFile(authToken, request)
            } catch (e: java.net.ConnectException) {
                // Backend not available, return mock response for development
                return@withContext Result.success(
                    UploadResponse(
                        archiveId = "mock-archive-${System.currentTimeMillis()}",
                        message = "Mock upload successful (backend not available)"
                    )
                )
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            }
            
            // Handle response
            when {
                response.isSuccessful -> {
                    response.body()?.let { uploadResponse ->
                        Result.success(uploadResponse)
                    } ?: Result.failure(Exception("Empty response from server"))
                }
                response.code() == 401 -> {
                    Result.failure(Exception("Authentication failed"))
                }
                response.code() == 413 -> {
                    Result.failure(Exception("File too large for server"))
                }
                else -> {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("Upload failed (${response.code()}): $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Upload error: ${e.message}"))
        }
    }
    
    suspend fun getArchiveList(
        token: String,
        limit: Int = 50,
        continuationToken: String? = null
    ): Result<ArchiveListResponse> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.getArchiveList(authToken, limit, continuationToken)
            
            if (response.isSuccessful) {
                response.body()?.let { archiveList ->
                    Log.d("ArchiveRepository", "Archive list received: ${archiveList.archives.size} items")
                    archiveList.archives.forEach { archive ->
                        Log.d("ArchiveRepository", "Archive: ${archive.fileName}, fileType: ${archive.fileType}, hasThumbnail: ${archive.hasThumbnail}")
                    }
                    Result.success(archiveList)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Log.e("ArchiveRepository", "Failed to get archive list: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to get archive list: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            // Backend not available, return mock data with thumbnail info for development
            Log.d("ArchiveRepository", "Backend not available, returning mock data")
            Result.success(
                ArchiveListResponse(
                    archives = listOf(
                        ArchiveItem(
                            archiveId = "mock-image-1",
                            fileName = "test-image.jpg",
                            fileSize = 1024000,
                            uploadTimestamp = kotlinx.datetime.Clock.System.now().toString(),
                            status = "ARCHIVED",
                            fileType = "image",
                            hasThumbnail = true,
                            thumbnailKey = "mock-thumbnail-key"
                        ),
                        ArchiveItem(
                            archiveId = "mock-video-1", 
                            fileName = "test-video.mp4",
                            fileSize = 5120000,
                            uploadTimestamp = kotlinx.datetime.Clock.System.now().toString(),
                            status = "ARCHIVED",
                            fileType = "video",
                            hasThumbnail = true,
                            thumbnailKey = "mock-thumbnail-key"
                        ),
                        ArchiveItem(
                            archiveId = "mock-doc-1",
                            fileName = "document.pdf",
                            fileSize = 512000,
                            uploadTimestamp = kotlinx.datetime.Clock.System.now().toString(),
                            status = "ARCHIVED",
                            fileType = "other",
                            hasThumbnail = false,
                            thumbnailKey = null
                        )
                    ),
                    continuationToken = null,
                    hasMore = false
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun restoreArchive(
        token: String,
        archiveId: String
    ): Result<RestoreResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getArchive("Bearer $token", archiveId)
            
            if (response.isSuccessful) {
                response.body()?.let { restoreResponse ->
                    // ファイルコンテンツがある場合（復元完了）、ファイルを保存
                    if (restoreResponse.content != null && restoreResponse.fileName != null) {
                        try {
                            saveRestoredFile(restoreResponse.fileName, restoreResponse.content)
                        } catch (e: Exception) {
                            Log.e("ArchiveRepository", "Failed to save restored file: ${e.message}")
                            // ファイル保存に失敗してもリストア情報は返す
                        }
                    }
                    Result.success(restoreResponse)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to restore archive: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun saveRestoredFile(fileName: String, base64Content: String) {
        try {
            // Base64デコード
            val fileBytes = Base64.decode(base64Content, Base64.DEFAULT)
            
            // Downloads フォルダに保存
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            
            // ファイル名の重複を避けるためタイムスタンプを追加
            val timestamp = System.currentTimeMillis()
            val fileExtension = fileName.substringAfterLast(".", "")
            val baseName = fileName.substringBeforeLast(".")
            val uniqueFileName = if (fileExtension.isNotEmpty()) {
                "${baseName}_restored_${timestamp}.${fileExtension}"
            } else {
                "${fileName}_restored_${timestamp}"
            }
            
            val file = java.io.File(downloadsDir, uniqueFileName)
            file.writeBytes(fileBytes)
            
            Log.d("ArchiveRepository", "File saved to: ${file.absolutePath}")
            
            // MediaScanConnectionを使用してファイルをメディアストアに追加
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.d("ArchiveRepository", "Media scan completed for: $path")
            }
            
        } catch (e: Exception) {
            Log.e("ArchiveRepository", "Error saving restored file: ${e.message}")
            throw e
        }
    }
    
    suspend fun deleteArchive(
        token: String,
        archiveId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteArchive("Bearer $token", archiveId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete archive: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getThumbnail(
        token: String,
        archiveId: String
    ): Result<ThumbnailResponse> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.getThumbnail(authToken, archiveId)
            
            if (response.isSuccessful) {
                response.body()?.let { thumbnailResponse ->
                    Log.d("ArchiveRepository", "Thumbnail URL received: ${thumbnailResponse.thumbnailUrl}")
                    Result.success(thumbnailResponse)
                } ?: Result.failure(Exception("Empty thumbnail response"))
            } else {
                when (response.code()) {
                    404 -> Result.failure(Exception("Thumbnail not available"))
                    401 -> Result.failure(Exception("Authentication failed"))
                    else -> Result.failure(Exception("Failed to get thumbnail: ${response.message()}"))
                }
            }
        } catch (e: java.net.ConnectException) {
            // Backend not available, return mock thumbnail URL for development
            Log.d("ArchiveRepository", "Backend not available, returning mock thumbnail URL for $archiveId")
            
            Result.success(
                ThumbnailResponse(
                    thumbnailUrl = "https://via.placeholder.com/200x200/4CAF50/FFFFFF?text=MOCK",
                    fileType = "image",
                    cacheInfo = CacheInfo(
                        maxAge = 3600,
                        provider = "Mock"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    private fun generateMockThumbnail(archiveId: String): ByteArray {
        // Create a simple colored bitmap
        val bitmap = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Use archiveId to generate consistent colors
        val hash = archiveId.hashCode()
        val red = (hash and 0xFF0000) shr 16
        val green = (hash and 0x00FF00) shr 8
        val blue = hash and 0x0000FF
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(
                (red + 100) % 256,
                (green + 100) % 256, 
                (blue + 100) % 256
            )
        }
        
        canvas.drawRect(0f, 0f, 200f, 200f, paint)
        
        // Add some text
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        canvas.drawText("MOCK", 100f, 90f, textPaint)
        canvas.drawText("THUMB", 100f, 120f, textPaint)
        
        // Convert to byte array
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }
}