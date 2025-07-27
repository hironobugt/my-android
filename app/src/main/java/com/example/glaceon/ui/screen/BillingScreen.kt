package com.example.glaceon.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.R
import com.example.glaceon.ui.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
        onNavigateToPaymentMethods: () -> Unit = {},
        onNavigateToUsage: () -> Unit = {},
        onNavigateToInvoices: () -> Unit = {},
        onNavigateToDeleteAccount: () -> Unit = {},
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
    val billingInfo by viewModel.billingInfo.collectAsState()
    val usage by viewModel.usage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.getAccessToken()?.let { token ->
            viewModel.loadBillingInfo(token)
            viewModel.loadUsage(token)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
                text = stringResource(R.string.billing_payments),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
        )

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
            // サブスクリプション情報
            item {
                SubscriptionCard(
                        billingInfo = billingInfo,
                        onCancelSubscription = {
                            authViewModel.getAccessToken()?.let { token ->
                                viewModel.cancelSubscription(token)
                            }
                        },
                        viewModel = viewModel
                )
            }

            // 使用量情報
            item {
                UsageCard(
                        usage = usage,
                        onNavigateToUsage = onNavigateToUsage,
                        viewModel = viewModel
                )
            }

            // 支払い方法
            item {
                PaymentMethodCard(
                        paymentMethods = billingInfo?.paymentMethods ?: emptyList(),
                        onNavigateToPaymentMethods = onNavigateToPaymentMethods
                )
            }

            // 請求書
            item { InvoiceCard(onNavigateToInvoices = onNavigateToInvoices) }
            
            // アカウント削除
            item { DeleteAccountCard(onNavigateToDeleteAccount = onNavigateToDeleteAccount) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCard(
        billingInfo: com.example.glaceon.ui.viewmodel.BillingInfo?,
        onCancelSubscription: () -> Unit,
        viewModel: BillingViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(R.string.subscription),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                billingInfo?.subscription?.let { subscription ->
                    val statusColor =
                            when (subscription.status) {
                                "active" -> MaterialTheme.colorScheme.primary
                                "canceled" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }

                    AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                        text =
                                                when (subscription.status) {
                                                    "active" -> stringResource(R.string.subscription_active)
                                                    "canceled" -> stringResource(R.string.subscription_canceled)
                                                    else -> subscription.status
                                                }
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(labelColor = statusColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            billingInfo?.subscription?.let { subscription ->
                Text(
                        text = stringResource(R.string.plan_glacier_archive),
                        style = MaterialTheme.typography.bodyMedium
                )
                Text(
                        text = stringResource(R.string.next_billing_date, viewModel.formatDate(subscription.currentPeriodEnd)),
                        style = MaterialTheme.typography.bodyMedium
                )

                if (subscription.cancelAtPeriodEnd) {
                    Text(
                            text = stringResource(R.string.will_be_canceled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!subscription.cancelAtPeriodEnd) {
                    OutlinedButton(
                            onClick = onCancelSubscription,
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                    )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cancel_subscription))
                    }
                }
            }
                    ?: run {
                        Text(
                                text = stringResource(R.string.no_subscription),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = { /* TODO: Navigate to subscription creation */}) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.start_subscription))
                        }
                    }
        }
    }
}

@Composable
fun UsageCard(
        usage: com.example.glaceon.ui.viewmodel.Usage?,
        onNavigateToUsage: () -> Unit = {},
        viewModel: BillingViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = stringResource(R.string.this_month_usage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            usage?.let { usage ->
                UsageItem(
                        icon = Icons.Default.Storage,
                        label = stringResource(R.string.storage),
                        value = "${String.format("%.1f", usage.storageGB)} GB",
                        cost = viewModel.formatCurrency(usage.costs["storage"] ?: 0.0)
                )

                UsageItem(
                        icon = Icons.Default.Upload,
                        label = stringResource(R.string.uploads),
                        value = "${usage.uploadCount} ${stringResource(R.string.times)}",
                        cost = viewModel.formatCurrency(usage.costs["uploads"] ?: 0.0)
                )

                UsageItem(
                        icon = Icons.Default.Download,
                        label = stringResource(R.string.restores),
                        value = "${usage.restoreCount} ${stringResource(R.string.times)}",
                        cost = viewModel.formatCurrency(usage.costs["restores"] ?: 0.0)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.base_fee), style = MaterialTheme.typography.bodyMedium)
                    Text(
                            text = viewModel.formatCurrency(usage.costs["baseFee"] ?: 0.0),
                            style = MaterialTheme.typography.bodyMedium
                    )
                }

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
                            text = viewModel.formatCurrency(usage.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToUsage, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.view_details))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                    )
                }
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
fun UsageItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        value: String,
        cost: String
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Text(text = cost, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PaymentMethodCard(
        paymentMethods: List<com.example.glaceon.ui.viewmodel.PaymentMethod>,
        onNavigateToPaymentMethods: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(R.string.payment_methods),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToPaymentMethods) { Text(stringResource(R.string.manage)) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (paymentMethods.isEmpty()) {
                Text(
                        text = stringResource(R.string.no_payment_methods),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onNavigateToPaymentMethods) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_payment_method))
                }
            } else {
                paymentMethods.forEach { paymentMethod ->
                    PaymentMethodItem(paymentMethod = paymentMethod)
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(paymentMethod: com.example.glaceon.ui.viewmodel.PaymentMethod) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))

        paymentMethod.card?.let { card ->
            Column {
                Text(
                        text = "${card.brand.uppercase()} •••• ${card.last4}",
                        style = MaterialTheme.typography.bodyMedium
                )
                Text(
                        text = "${card.expMonth}/${card.expYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun InvoiceCard(onNavigateToInvoices: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(R.string.invoices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToInvoices) { Text(stringResource(R.string.view_all)) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.latest_invoices), style = MaterialTheme.typography.bodyMedium)
                    Text(
                            text = stringResource(R.string.view_payment_history),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                    )
                }
                Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
@Composable
fun DeleteAccountCard(onNavigateToDeleteAccount: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Permanently delete your account and all data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToDeleteAccount,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete My Account")
            }
        }
    }
}