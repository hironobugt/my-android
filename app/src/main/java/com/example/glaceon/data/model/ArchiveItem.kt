package com.example.glaceon.data.model

import kotlinx.datetime.Instant

data class ArchiveItem(
    val archiveId: String,
    val fileName: String,
    val fileSize: Long,
    val uploadTimestamp: String, // バックエンドの形式に合わせる
    val status: String = "ARCHIVED", // バックエンドは文字列で返す
    val storageClass: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val s3Key: String? = null,
    val fileType: String = "other",
    val hasThumbnail: Boolean = false,
    val thumbnailKey: String? = null,
    // 後方互換性のため
    val description: String? = null,
    val category: String? = null
) {
    // uploadDateプロパティを計算プロパティとして提供（後方互換性）
    val uploadDate: Instant
        get() = try {
            kotlinx.datetime.Instant.parse(uploadTimestamp)
        } catch (e: Exception) {
            kotlinx.datetime.Clock.System.now()
        }
    
    // statusをenumとして取得（後方互換性）
    val archiveStatus: ArchiveStatus
        get() = when (status.uppercase()) {
            "UPLOADING" -> ArchiveStatus.UPLOADING
            "ARCHIVED" -> ArchiveStatus.ARCHIVED
            "RESTORING" -> ArchiveStatus.RESTORING
            "RESTORED" -> ArchiveStatus.RESTORED
            "FAILED" -> ArchiveStatus.FAILED
            else -> ArchiveStatus.ARCHIVED
        }
}

enum class ArchiveStatus {
    UPLOADING,
    ARCHIVED,
    RESTORING,
    RESTORED,
    FAILED
}

data class UploadRequest(
    val fileName: String,
    val fileContent: String, // Base64 encoded
    val metadata: Map<String, String> = emptyMap()
)

data class UploadResponse(
    val archiveId: String,
    val message: String
)

data class ArchiveListResponse(
    val archives: List<ArchiveItem>,
    val continuationToken: String? = null,
    val hasMore: Boolean = false
)

data class RestoreResponse(
    val archiveId: String,
    val status: String,
    val message: String,
    val downloadUrl: String? = null,
    val expiresAt: String? = null,
    val fileName: String? = null,
    val content: String? = null,
    val contentType: String? = null,
    val fileSize: Long? = null
)

data class ThumbnailResponse(
    val thumbnailUrl: String,
    val fileType: String,
    val cacheInfo: CacheInfo? = null
)

data class CacheInfo(
    val maxAge: Long,
    val provider: String
)

data class DownloadResponse(
    val status: String,
    val message: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val downloadUrl: String? = null,
    val content: String? = null, // Base64 encoded file content
    val contentType: String? = null,
    val expiresAt: String? = null
)