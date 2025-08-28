package com.example.glaceon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.glaceon.ui.screen.ApiTestScreen
import com.example.glaceon.ui.screen.ArchiveListScreen
import com.example.glaceon.ui.screen.AutoUploadSettingsScreen
import com.example.glaceon.ui.screen.AutoUploadTestScreen
import com.example.glaceon.ui.screen.BillingScreen
import com.example.glaceon.ui.screen.DeleteAccountScreen
import com.example.glaceon.ui.screen.ForgotPasswordScreen
import com.example.glaceon.ui.screen.InvoiceScreen
import com.example.glaceon.ui.screen.LoginScreen
import com.example.glaceon.ui.screen.PaymentMethodScreen
import com.example.glaceon.ui.screen.PermissionScreen
import com.example.glaceon.ui.screen.RegisterScreen
import com.example.glaceon.ui.screen.UsageScreen
import com.example.glaceon.ui.theme.GlaceonTheme
import com.example.glaceon.ui.viewmodel.AuthViewModel
import com.example.glaceon.ui.viewmodel.PermissionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start file monitoring service for auto-upload
        com.example.glaceon.service.FileMonitorService.startService(this)
        
        setContent { GlaceonTheme { GlaceonApp() } }
    }
}

@Composable
fun GlaceonApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val permissionViewModel: PermissionViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val permissionState by permissionViewModel.permissionState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 初回起動時に権限チェックを実行
    LaunchedEffect(Unit) {
        permissionViewModel.checkPermissions(context)
    }

    // Determine start destination based on auth and permission state
    val startDestination = when {
        !permissionState.permissionCheckComplete -> "permissions" // 権限チェック中は権限画面を表示
        !permissionState.hasRequiredPermissions -> "permissions"
        authState.isAuthenticated -> "archive_list"
        authState.needsConfirmation -> "register" // 認証コード入力画面に戻る
        authState.needsPasswordReset -> "forgot_password" // パスワードリセット画面に戻る
        else -> "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("permissions") {
            PermissionScreen(
                onPermissionsGranted = {
                    // 権限が許可されたら認証状態に応じて適切な画面に遷移
                    val nextDestination = when {
                        authState.isAuthenticated -> "archive_list"
                        authState.needsConfirmation -> "register"
                        authState.needsPasswordReset -> "forgot_password"
                        else -> "login"
                    }
                    navController.navigate(nextDestination) {
                        popUpTo("permissions") { inclusive = true }
                    }
                },
                permissionViewModel = permissionViewModel
            )
        }

        composable("login") {
            LoginScreen(
                    onNavigateToRegister = {
                        authViewModel.resetRegistrationState()
                        navController.navigate("register")
                    },
                    onNavigateToForgotPassword = {
                        authViewModel.resetAuthState()
                        navController.navigate("forgot_password")
                    },
                    onLoginSuccess = {
                        navController.navigate("archive_list") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
            )
        }

        composable("register") {
            RegisterScreen(
                    onNavigateToLogin = {
                        navController.navigate("login") { popUpTo("register") { inclusive = true } }
                    },
                    authViewModel = authViewModel
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                    onNavigateToLogin = {
                        navController.navigate("login") { popUpTo("forgot_password") { inclusive = true } }
                    },
                    authViewModel = authViewModel
            )
        }

        composable("archive_list") {
            ArchiveListScreen(
                    onNavigateToSettings = { navController.navigate("auto_upload_settings") },
                    onNavigateToApiTest = { navController.navigate("api_test") },
                    onNavigateToBilling = { navController.navigate("billing") },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo("archive_list") { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
            )
        }

        composable("auto_upload_settings") {
            AutoUploadSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTest = { navController.navigate("auto_upload_test") }
            )
        }

        composable("auto_upload_test") {
            AutoUploadTestScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("api_test") { ApiTestScreen(onNavigateBack = { navController.popBackStack() }) }

        composable("billing") {
            BillingScreen(
                    onNavigateToPaymentMethods = { navController.navigate("payment_methods") },
                    onNavigateToUsage = { navController.navigate("usage") },
                    onNavigateToInvoices = { navController.navigate("invoices") },
                    onNavigateToDeleteAccount = { navController.navigate("delete_account") },
                    authViewModel = authViewModel
            )
        }

        composable("payment_methods") {
            PaymentMethodScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
            )
        }

        composable("usage") {
            UsageScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
            )
        }

        composable("invoices") {
            InvoiceScreen(
                    onNavigateBack = { navController.popBackStack() },
                    authViewModel = authViewModel
            )
        }

        composable("delete_account") {
            DeleteAccountScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAccountDeleted = {
                        // アカウント削除完了後はログイン画面に戻る
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
            )
        }
    }
}
