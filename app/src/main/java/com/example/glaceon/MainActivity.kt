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
import com.example.glaceon.ui.screen.InvoiceScreen
import com.example.glaceon.ui.screen.LoginScreen
import com.example.glaceon.ui.screen.PaymentMethodScreen
import com.example.glaceon.ui.screen.RegisterScreen
import com.example.glaceon.ui.screen.UsageScreen
import com.example.glaceon.ui.theme.GlaceonTheme
import com.example.glaceon.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlaceonTheme { GlaceonApp() } }
    }
}

@Composable
fun GlaceonApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = if (authState.isAuthenticated) "archive_list" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                    onNavigateToRegister = {
                        authViewModel.resetRegistrationState()
                        navController.navigate("register")
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
    }
}
