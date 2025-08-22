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
                println("PaymentMethodScreen: PaymentSheet失敗: ${result.error}")
                viewModel.clearError()
            }
        }
    }

    // Stripe設定の初期化状態を管理
    var isStripeInitialized by remember { mutableStateOf(false) }
    
    // Stripe設定を直接初期化（一時的にハードコード）
    LaunchedEffect(Unit) {
        println("PaymentMethodScreen: Stripe設定を初期化中...")
        try {
            // 実際のStripe公開可能キーを直接使用
            val publishableKey = "pk_test_51RrIiS3jPENBfOzdq69mzsLq5tvHNOU5Xphn36HstnfQL2gxRUVcjKbvZsUxRfxfm5IfKosn8Bvtjc3QrfgM26N000jqWHmyBG"
            PaymentConfiguration.init(context, publishableKey)
            println("PaymentMethodScreen: PaymentConfiguration初期化完了")
            isStripeInitialized = true
        } catch (e: Exception) {
            println("PaymentMethodScreen: PaymentConfiguration初期化エラー: ${e.message}")
            isStripeInitialized = false
        }
        
        // billing情報をロード（認証必要）
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
                println("PaymentMethodScreen: カード追加ボタンがクリックされました")
                
                // Stripeが初期化されているかチェック
                if (!isStripeInitialized) {
                    println("PaymentMethodScreen: Stripeが初期化されていません")
                    return@Button
                }
                
                authViewModel.getAccessToken()?.let { token ->
                    authViewModel.currentUser.value?.let { user ->
                        println("PaymentMethodScreen: トークンとユーザー情報を取得しました")
                        
                        // まず顧客が存在するか確認し、存在しない場合は作成
                        if (billingInfo?.customer?.stripeCustomerId == null) {
                            println("PaymentMethodScreen: 顧客が存在しないため、まず顧客を作成します")
                            viewModel.setupCustomer(token, user.email, user.username)
                            // 顧客作成後は自動的にbilling情報が再読み込みされるので、
                            // 次回ボタンを押した時にカード登録が実行される
                            return@let
                        }
                        
                        // 顧客が存在する場合、Setup Intentを作成
                        viewModel.createPaymentIntent(token) { clientSecret ->
                            println("PaymentMethodScreen: clientSecret取得: $clientSecret")
                            
                            // PaymentConfigurationが初期化されているか再確認
                            try {
                                PaymentConfiguration.getInstance(context)
                                println("PaymentMethodScreen: PaymentConfiguration確認OK")
                            } catch (e: Exception) {
                                println("PaymentMethodScreen: PaymentConfiguration未初期化: ${e.message}")
                                return@createPaymentIntent
                            }
                            
                            val configuration = PaymentSheet.Configuration(
                                merchantDisplayName = "Glacier Archive"
                            )
                            println("PaymentMethodScreen: PaymentSheetを起動します")
                            try {
                                paymentSheetLauncher.launch(
                                    PaymentSheetContract.Args.createSetupIntentArgs(
                                        clientSecret = clientSecret,
                                        config = configuration
                                    )
                                )
                                println("PaymentMethodScreen: PaymentSheet起動成功")
                            } catch (e: Exception) {
                                println("PaymentMethodScreen: PaymentSheet起動エラー: ${e.message}")
                            }
                        }
                    } ?: run {
                        println("PaymentMethodScreen: ユーザー情報が取得できませんでした")
                    }
                } ?: run {
                    println("PaymentMethodScreen: トークンが取得できませんでした")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && isStripeInitialized
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else if (!isStripeInitialized) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                when {
                    !isStripeInitialized -> "Stripe設定を初期化中..."
                    billingInfo?.customer?.stripeCustomerId == null -> "顧客情報を設定してカードを追加"
                    else -> "新しいカードを追加"
                }
            )
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