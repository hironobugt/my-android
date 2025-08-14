package com.example.glaceon.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    onNavigateBack: () -> Unit = {},
    authViewModel: com.example.glaceon.ui.viewmodel.AuthViewModel,
    viewModel: BillingViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val context = LocalContext.current
    val billingInfo by viewModel.billingInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Stripe PaymentSheet launcher
    val paymentSheetLauncher = rememberLauncherForActivityResult(
        contract = PaymentSheetContract()
    ) { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                // カード登録成功
                authViewModel.getAccessToken()?.let { token ->
                    authViewModel.currentUser.value?.let { user ->
                        viewModel.loadBillingInfo(token, user.email, user.username)
                    }
                }
            }
            is PaymentSheetResult.Canceled -> {
                // ユーザーがキャンセル
            }
            is PaymentSheetResult.Failed -> {
                // エラー処理 - 具体的なエラーメッセージを表示
                // result.error?.message を使用してエラー詳細を取得可能
            }
        }
    }

    // Stripe設定を取得して初期化
    LaunchedEffect(Unit) {
        // Stripe設定を取得
        viewModel.getStripeConfig { publishableKey ->
            PaymentConfiguration.init(context, publishableKey)
        }
        
        authViewModel.getAccessToken()?.let { token ->
            authViewModel.currentUser.value?.let { user ->
                viewModel.loadBillingInfo(token, user.email, user.username)
            }
        }
    }

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
                Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
            }
            Text(
                text = stringResource(R.string.payment_methods),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
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
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // カード追加ボタン
        Button(
            onClick = {
                authViewModel.getAccessToken()?.let { token ->
                    viewModel.createPaymentIntent(token) { clientSecret ->
                        val configuration = PaymentSheet.Configuration(
                            merchantDisplayName = "Glacier Archive",
                            customer = billingInfo?.customer?.stripeCustomerId?.let { customerId ->
                                PaymentSheet.CustomerConfiguration(
                                    id = customerId,
                                    ephemeralKeySecret = "" // 空文字列でも動作する
                                )
                            }
                        )
                        paymentSheetLauncher.launch(
                            PaymentSheetContract.Args.createSetupIntentArgs(
                                clientSecret = clientSecret,
                                config = configuration
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新しいカードを追加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 既存の支払い方法一覧
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(billingInfo?.paymentMethods ?: emptyList()) { paymentMethod ->
                PaymentMethodItem(
                    paymentMethod = paymentMethod,
                    onRemove = {
                        authViewModel.getAccessToken()?.let { token ->
                            viewModel.removePaymentMethod(token, paymentMethod.id)
                        }
                    }
                )
            }
        }

        if (billingInfo?.paymentMethods?.isEmpty() == true) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "登録されている支払い方法がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(
    paymentMethod: com.example.glaceon.ui.viewmodel.PaymentMethod,
    onRemove: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    paymentMethod.card?.let { card ->
                        Text(
                            text = "${card.brand.uppercase()} •••• ${card.last4}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${card.expMonth}/${card.expYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } ?: run {
                        Text(
                            text = paymentMethod.type,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}