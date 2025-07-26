package com.example.glaceon.data.repository

import android.content.Context
import android.util.Log
import com.example.glaceon.data.api.ApiClient
import com.example.glaceon.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BillingRepository(private val context: Context) {
    
    private val api = ApiClient.glaceonApi
    
    suspend fun getBillingInfo(token: String): Result<BillingData> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "get-billing-info")
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success && billingResponse.data != null) {
                        Result.success(billingResponse.data)
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Unknown billing error"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Authentication failed"))
                    404 -> Result.failure(Exception("Customer not found"))
                    else -> Result.failure(Exception("Failed to get billing info: ${response.message()}"))
                }
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, returning mock billing data")
            // Return mock data for development
            Result.success(createMockBillingData())
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun getUsage(token: String, month: String? = null): Result<UsageData> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = UsageRequest(month = month)
            val response = api.getUsage(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { usageResponse ->
                    if (usageResponse.success && usageResponse.data != null) {
                        Result.success(usageResponse.data)
                    } else {
                        Result.failure(Exception(usageResponse.error ?: "Unknown usage error"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get usage: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, returning mock usage data")
            // Return mock data for development
            Result.success(createMockUsageData())
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun getInvoices(token: String, limit: Int = 10): Result<List<InvoiceData>> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "get-invoices", limit = limit)
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success && billingResponse.data?.invoices != null) {
                        Result.success(billingResponse.data.invoices)
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Unknown invoice error"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get invoices: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, returning mock invoice data")
            // Return mock data for development
            Result.success(createMockInvoiceData())
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun setupCustomer(token: String, email: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "setup-customer", email = email, name = name)
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success && billingResponse.data?.customerId != null) {
                        Result.success(billingResponse.data.customerId)
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to setup customer"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to setup customer: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun addPaymentMethod(token: String, paymentMethodId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "add-payment-method", paymentMethodId = paymentMethodId)
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success) {
                        Result.success(billingResponse.data?.message ?: "Payment method added")
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to add payment method"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to add payment method: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun createSubscription(token: String, priceId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "create-subscription", priceId = priceId)
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success && billingResponse.data?.subscriptionId != null) {
                        Result.success(billingResponse.data.subscriptionId)
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to create subscription"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to create subscription: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun cancelSubscription(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "cancel-subscription")
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success) {
                        Result.success(billingResponse.data?.message ?: "Subscription cancelled")
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to cancel subscription"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to cancel subscription: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    // Mock data for development when backend is not available
    private fun createMockBillingData(): BillingData {
        return BillingData(
            customer = CustomerData(
                userId = "user123",
                stripeCustomerId = "cus_123",
                email = "user@example.com",
                name = "Test User",
                subscriptionStatus = "active"
            ),
            subscription = SubscriptionData(
                id = "sub_123",
                status = "active",
                currentPeriodStart = System.currentTimeMillis() / 1000 - 86400 * 15,
                currentPeriodEnd = System.currentTimeMillis() / 1000 + 86400 * 15,
                cancelAtPeriodEnd = false
            ),
            paymentMethods = listOf(
                PaymentMethodData(
                    id = "pm_123",
                    type = "card",
                    card = CardData("visa", "4242", 12, 2025)
                )
            )
        )
    }
    
    private fun createMockUsageData(): UsageData {
        return UsageData(
            usage = UsageInfo(
                userId = "user123",
                periodMonth = "2024-01",
                storageGB = 15.5,
                uploadCount = 25,
                restoreCount = 3,
                totalCost = 8.75
            ),
            costs = mapOf(
                "storage" to 0.16,
                "uploads" to 1.25,
                "restores" to 0.30,
                "baseFee" to 5.00
            ),
            totalCost = 8.75,
            pricing = PricingInfo(
                storage = 0.01,
                upload = 0.05,
                restore = 0.10,
                baseFee = 5.00
            )
        )
    }
    
    private fun createMockInvoiceData(): List<InvoiceData> {
        return listOf(
            InvoiceData(
                id = "in_123",
                amountPaid = 875,
                amountDue = 0,
                currency = "usd",
                status = "paid",
                created = System.currentTimeMillis() / 1000 - 86400 * 30,
                periodStart = System.currentTimeMillis() / 1000 - 86400 * 60,
                periodEnd = System.currentTimeMillis() / 1000 - 86400 * 30,
                hostedInvoiceUrl = "https://invoice.stripe.com/i/123",
                invoicePdf = "https://invoice.stripe.com/i/123.pdf"
            )
        )
    }
}