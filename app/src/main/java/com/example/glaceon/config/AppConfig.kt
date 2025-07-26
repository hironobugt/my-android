package com.example.glaceon.config

import com.example.glaceon.BuildConfig

/** アプリケーション設定を管理するクラス 環境変数、BuildConfig、デフォルト値を統合管理 */
object AppConfig {

    /** API Base URL 優先順位: BuildConfig > デフォルト値 */
    val API_BASE_URL: String = BuildConfig.API_BASE_URL

    /** ビルドタイプ */
    val BUILD_TYPE: String = BuildConfig.BUILD_TYPE

    /** デバッグモードかどうか */
    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    /** ログレベル設定 */
    val LOG_LEVEL: LogLevel = if (IS_DEBUG) LogLevel.BODY else LogLevel.BASIC

    /** ネットワークタイムアウト設定（秒） */
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L

    /** ファイルアップロード設定 */
    const val MAX_FILE_SIZE_MB = 10
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

    /** 自動アップロード設定 */
    const val AUTO_UPLOAD_CHECK_INTERVAL_MINUTES = 15L

    /** キャッシュ設定 */
    const val THUMBNAIL_CACHE_SIZE = 50 // 最大50個のサムネイルをキャッシュ

    /** ログレベル列挙型 */
    enum class LogLevel {
        NONE, // ログなし
        BASIC, // 基本ログ（リクエスト/レスポンスライン）
        HEADERS, // ヘッダー付きログ
        BODY // 全ログ（ボディ含む）
    }

    /** 環境別設定 */
    object Environment {
        val isDevelopment: Boolean = BUILD_TYPE == "DEBUG"
        val isProduction: Boolean = BUILD_TYPE == "RELEASE"
        val isStaging: Boolean = BUILD_TYPE == "STAGING"

        /** 環境名を取得 */
        fun getEnvironmentName(): String =
                when {
                    isDevelopment -> "Development"
                    isStaging -> "Staging"
                    isProduction -> "Production"
                    else -> "Unknown"
                }
    }

    /** デバッグ情報を取得 */
    fun getDebugInfo(): Map<String, String> =
            mapOf(
                    "API_BASE_URL" to API_BASE_URL,
                    "BUILD_TYPE" to BUILD_TYPE,
                    "IS_DEBUG" to IS_DEBUG.toString(),
                    "ENVIRONMENT" to Environment.getEnvironmentName(),
                    "LOG_LEVEL" to LOG_LEVEL.name
            )
}
