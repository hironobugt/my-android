package com.example.glaceon.ui.screen

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glaceon.R
import com.example.glaceon.ui.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
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
    val invoices by viewModel.invoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        authViewModel.getAccessToken()?.let { token -> viewModel.loadInvoices(token) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // ヘッダー
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                    text = stringResource(R.string.invoices),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
            )
        }

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

        if (invoices.isEmpty() && !isLoading) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            text = stringResource(R.string.no_invoices),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = stringResource(R.string.no_invoices_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(invoices) { invoice ->
                    InvoiceCard(
                            invoice = invoice,
                            viewModel = viewModel,
                            onViewInvoice = { url -> url?.let { uriHandler.openUri(it) } },
                            onDownloadPdf = { url -> url?.let { uriHandler.openUri(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
        invoice: com.example.glaceon.ui.viewmodel.Invoice,
        viewModel: BillingViewModel,
        onViewInvoice: (String?) -> Unit,
        onDownloadPdf: (String?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ヘッダー行
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                            text = stringResource(R.string.invoice_number, invoice.id.takeLast(8)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = viewModel.formatDate(invoice.created),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                    )
                }

                InvoiceStatusChip(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Billing period
            Text(
                    text = stringResource(R.string.billing_period, viewModel.formatDate(invoice.periodStart), viewModel.formatDate(invoice.periodEnd)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount information
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                            text = stringResource(R.string.paid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                            text = viewModel.formatCurrency(invoice.amountPaid),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color =
                                    if (invoice.amountDue > 0) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.primary
                    )
                }

                if (invoice.amountDue > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                                text = stringResource(R.string.due),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                        )
                        Text(
                                text = viewModel.formatCurrency(invoice.amountDue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                        onClick = { onViewInvoice(invoice.hostedInvoiceUrl) },
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.view))
                }

                OutlinedButton(
                        onClick = { onDownloadPdf(invoice.invoicePdf) },
                        modifier = Modifier.weight(1f)
                ) {
                    Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.pdf))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceStatusChip(status: String) {
    val (text, color) =
            when (status) {
                "paid" -> stringResource(R.string.invoice_status_paid) to MaterialTheme.colorScheme.primary
                "open" -> stringResource(R.string.invoice_status_open) to MaterialTheme.colorScheme.error
                "draft" -> stringResource(R.string.invoice_status_draft) to MaterialTheme.colorScheme.outline
                "void" -> stringResource(R.string.invoice_status_void) to MaterialTheme.colorScheme.outline
                else -> status to MaterialTheme.colorScheme.outline
            }

    AssistChip(
            onClick = {},
            label = { Text(text) },
            colors = AssistChipDefaults.assistChipColors(labelColor = color),
            leadingIcon = {
                Icon(
                        when (status) {
                            "paid" -> Icons.Default.CheckCircle
                            "open" -> Icons.Default.Warning
                            "draft" -> Icons.Default.Edit
                            "void" -> Icons.Default.Cancel
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                )
            }
    )
}
