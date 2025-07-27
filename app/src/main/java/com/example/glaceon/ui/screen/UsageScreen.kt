package com.example.glaceon.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glaceon.R
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.ui.viewmodel.BillingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
        onNavigateBack: () -> Unit,
        authViewModel: com.example.glaceon.ui.viewmodel.AuthViewModel,
        viewModel: BillingViewModel =
                viewModel(
                        factory =
                                ViewModelProvider.AndroidViewModelFactory.getInstance(
                                        LocalContext.current.applicationContext as
                                                android.app.Application
                                )
                )
) {
    val usage by viewModel.usage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedMonth by remember { mutableStateOf(getCurrentMonth()) }

    LaunchedEffect(selectedMonth) {
        authViewModel.getAccessToken()?.let { token -> viewModel.loadUsage(token, selectedMonth) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // ヘッダー
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                    text = stringResource(R.string.usage_billing),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 月選択
        MonthSelector(selectedMonth = selectedMonth, onMonthSelected = { selectedMonth = it })

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error?.let { errorMessage ->
            Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                            )
            ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 合計料金カード
            item { TotalCostCard(usage = usage) }

            // 使用量詳細カード
            item { UsageDetailCard(usage = usage) }

            // 料金内訳カード
            item { CostBreakdownCard(usage = usage) }

            // 料金体系説明カード
            item { PricingInfoCard() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(selectedMonth: String, onMonthSelected: (String) -> Unit) {
    val months = remember {
        (0..11).map { offset ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -offset)
            val monthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val displayStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time)
            monthStr to displayStr
        }
    }

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
                value = months.find { it.first == selectedMonth }?.second ?: selectedMonth,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.target_month)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { (monthValue, monthDisplay) ->
                DropdownMenuItem(
                        text = { Text(monthDisplay) },
                        onClick = {
                            onMonthSelected(monthValue)
                            expanded = false
                        }
                )
            }
        }
    }
}

@Composable
fun TotalCostCard(usage: com.example.glaceon.ui.viewmodel.Usage?) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = stringResource(R.string.this_month_total_cost),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                    text = usage?.let { String.format("$%.2f", it.totalCost) } ?: "$0.00",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun UsageDetailCard(usage: com.example.glaceon.ui.viewmodel.Usage?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = stringResource(R.string.usage_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            usage?.let { usage ->
                UsageDetailItem(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.storage_usage),
                        value = "${String.format("%.1f", usage.storageGB)} GB",
                        description = stringResource(R.string.storage_usage_desc)
                )

                UsageDetailItem(
                        icon = Icons.Default.Upload,
                        title = stringResource(R.string.upload_count),
                        value = "${usage.uploadCount} ${stringResource(R.string.times)}",
                        description = stringResource(R.string.upload_count_desc)
                )

                UsageDetailItem(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.restore_count),
                        value = "${usage.restoreCount} ${stringResource(R.string.times)}",
                        description = stringResource(R.string.restore_count_desc)
                )
            }
                    ?: run {
                        Text(
                                text = stringResource(R.string.loading_usage_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
        }
    }
}

@Composable
fun UsageDetailItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        value: String,
        description: String
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CostBreakdownCard(usage: com.example.glaceon.ui.viewmodel.Usage?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = stringResource(R.string.cost_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            usage?.let { usage ->
                CostBreakdownItem(
                        label = stringResource(R.string.base_fee),
                        amount = usage.costs["baseFee"] ?: 0.0,
                        description = stringResource(R.string.base_fee_desc)
                )

                CostBreakdownItem(
                        label = stringResource(R.string.storage_fee),
                        amount = usage.costs["storage"] ?: 0.0,
                        description = "${String.format("%.1f", usage.storageGB)} GB × $0.012"
                )

                CostBreakdownItem(
                        label = stringResource(R.string.upload_fee),
                        amount = usage.costs["uploads"] ?: 0.0,
                        description = "${usage.uploadCount} ${stringResource(R.string.times)} (${String.format("%.2f", usage.uploadGB)} GB × $0.09)"
                )

                CostBreakdownItem(
                        label = stringResource(R.string.restore_fee),
                        amount = usage.costs["restores"] ?: 0.0,
                        description = "${usage.restoreCount} ${stringResource(R.string.times)} (${String.format("%.2f", usage.restoreGB)} GB × $0.40)"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text = stringResource(R.string.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = String.format("$%.2f", usage.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                    )
                }
            }
                    ?: run {
                        Text(
                                text = stringResource(R.string.loading_cost_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
        }
    }
}

@Composable
fun CostBreakdownItem(label: String, amount: Double, description: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
                text = String.format("$%.2f", amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PricingInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = stringResource(R.string.pricing_structure),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PricingInfoItem(
                    label = stringResource(R.string.base_fee), 
                    price = stringResource(R.string.base_fee_price), 
                    description = stringResource(R.string.base_fee_price_desc)
            )

            PricingInfoItem(
                    label = stringResource(R.string.storage),
                    price = stringResource(R.string.storage_price),
                    description = stringResource(R.string.storage_price_desc)
            )

            PricingInfoItem(
                    label = stringResource(R.string.uploads), 
                    price = stringResource(R.string.upload_price), 
                    description = stringResource(R.string.upload_price_desc)
            )

            PricingInfoItem(
                    label = stringResource(R.string.restores), 
                    price = stringResource(R.string.restore_price), 
                    description = stringResource(R.string.restore_price_desc)
            )
        }
    }
}

@Composable
fun PricingInfoItem(label: String, price: String, description: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun getCurrentMonth(): String {
    return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
}
