package com.example.glaceon.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glaceon.config.AppConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit
) {
    val debugInfo = AppConfig.getDebugInfo()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Debug Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 環境情報カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (AppConfig.IS_DEBUG) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Environment: ${AppConfig.Environment.getEnvironmentName()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (AppConfig.IS_DEBUG) {
                        "Debug mode is enabled. Detailed logging is active."
                    } else {
                        "Production mode. Limited logging for performance."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 設定一覧
        Text(
            text = "Configuration Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(debugInfo.toList()) { (key, value) ->
                ConfigItem(key = key, value = value)
            }
            
            // 追加の設定情報
            item {
                ConfigItem(
                    key = "MAX_FILE_SIZE_MB", 
                    value = AppConfig.MAX_FILE_SIZE_MB.toString()
                )
            }
            
            item {
                ConfigItem(
                    key = "CONNECT_TIMEOUT", 
                    value = "${AppConfig.CONNECT_TIMEOUT}s"
                )
            }
            
            item {
                ConfigItem(
                    key = "READ_TIMEOUT", 
                    value = "${AppConfig.READ_TIMEOUT}s"
                )
            }
            
            item {
                ConfigItem(
                    key = "WRITE_TIMEOUT", 
                    value = "${AppConfig.WRITE_TIMEOUT}s"
                )
            }
            
            item {
                ConfigItem(
                    key = "THUMBNAIL_CACHE_SIZE", 
                    value = AppConfig.THUMBNAIL_CACHE_SIZE.toString()
                )
            }
            
            item {
                ConfigItem(
                    key = "AUTO_UPLOAD_CHECK_INTERVAL", 
                    value = "${AppConfig.AUTO_UPLOAD_CHECK_INTERVAL_MINUTES}min"
                )
            }
        }
    }
}

@Composable
fun ConfigItem(
    key: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}