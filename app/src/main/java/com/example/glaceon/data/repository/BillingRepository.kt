package com.example.glaceon.data.repository

import android.content.Context
import android.util.Log
import com.example.glaceon.data.api.ApiClient
import com.example.glaceon.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            val request = UsageRequest(action = "get-current-usage", month = month)
            val response = api.getUsage(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { usageResponse ->
                    if (usageResponse.error != null) {
                        Result.failure(Exception(usageResponse.error))
                    } else {
                        // usage APIレスポンスをUsageDataに変換
                        val usage = usageResponse.usage ?: UsageInfo(
                            userId = "",
                            periodMonth = "",
                            uploadCount = 0
                        )
                        val costs = usageResponse.costs ?: emptyMap()
                        val totalCost = costs.values.sum()
                        
                        val usageData = UsageData(
                            usage = usage.copy(totalCost = totalCost),
                            costs = costs,
                            totalCost = totalCost,
                            pricing = PricingInfo(
                                storage = 0.012,
                                upload = 0.09,
                                restore = 0.40,
                                baseFee = 3.00
                            )
                        )
                        Result.success(usageData)
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
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, simulating customer setup")
            delay(1000)
            Result.success("cus_mock_${System.currentTimeMillis()}")
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
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, simulating payment method addition")
            delay(1000)
            Result.success("Payment method added successfully")
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun removePaymentMethod(token: String, paymentMethodId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(action = "remove-payment-method", paymentMethodId = paymentMethodId)
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success) {
                        Result.success(billingResponse.data?.message ?: "Payment method removed")
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to remove payment method"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to remove payment method: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, simulating payment method removal")
            delay(1000)
            Result.success("Payment method removed successfully")
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
                storage = 0.012,
                upload = 0.09,
                restore = 0.40,
                baseFee = 3.00
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
    
    private fun createMockUsageHistoryData(): List<UsageData> {
        val currentTime = System.currentTimeMillis()
        return (0..11).map { monthsAgo ->
            val periodMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(java.util.Date(currentTime - monthsAgo * 30L * 24 * 60 * 60 * 1000))
            
            UsageData(
                usage = UsageInfo(
                    userId = "user123",
                    periodMonth = periodMonth,
                    storageGB = 10.0 + monthsAgo * 2.5,
                    uploadCount = 15 + monthsAgo * 3,
                    restoreCount = monthsAgo / 2,
                    uploadGB = 0.5 + monthsAgo * 0.2,
                    apiRequestCount = 50 + monthsAgo * 10,
                    thumbnailViews = 100 + monthsAgo * 20,
                    totalCost = 5.0 + monthsAgo * 1.2
                ),
                costs = mapOf(
                    "storage" to (0.12 + monthsAgo * 0.03),
                    "uploads" to (0.75 + monthsAgo * 0.15),
                    "restores" to (monthsAgo * 0.1),
                    "thumbnails" to (0.5 + monthsAgo * 0.1),
                    "baseFee" to 3.0
                ),
                totalCost = 5.0 + monthsAgo * 1.2,
                pricing = PricingInfo(
                    storage = 0.012,
                    upload = 0.09,
                    restore = 0.40,
                    baseFee = 3.00
                )
            )
        }
    }
    
    private fun createMockUsageEventsData(): List<UsageEvent> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            UsageEvent(
                id = "evt_1",
                userId = "user123",
                eventType = "upload",
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    .format(java.util.Date(currentTime - 3600000)),
                eventData = mapOf(
                    "fileName" to "document.pdf",
                    "fileSize" to 1024000,
                    "archiveId" to "arch_123"
                ),
                cost = 0.09
            ),
            UsageEvent(
                id = "evt_2",
                userId = "user123",
                eventType = "thumbnail_view",
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    .format(java.util.Date(currentTime - 7200000)),
                eventData = mapOf(
                    "archiveId" to "arch_123",
                    "thumbnailKey" to "thumb_123"
                ),
                cost = 0.005
            ),
            UsageEvent(
                id = "evt_3",
                userId = "user123",
                eventType = "restore",
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    .format(java.util.Date(currentTime - 86400000)),
                eventData = mapOf(
                    "archiveId" to "arch_456",
                    "fileName" to "image.jpg",
                    "fileSize" to 2048000
                ),
                cost = 0.40
            )
        )
    }
    
    suspend fun getUsageHistory(token: String, months: Int = 12): Result<List<UsageData>> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = UsageRequest(action = "get-usage-history", month = months.toString())
            val response = api.getUsage(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { usageResponse ->
                    if (usageResponse.error != null) {
                        Result.failure(Exception(usageResponse.error))
                    } else {
                        // Mock implementation for development
                        val historyData = createMockUsageHistoryData()
                        Result.success(historyData)
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get usage history: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, returning mock usage history")
            Result.success(createMockUsageHistoryData())
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun getUsageEvents(token: String, limit: Int = 50): Result<List<UsageEvent>> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = UsageRequest(action = "get-usage-events", month = limit.toString())
            val response = api.getUsage(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { usageResponse ->
                    if (usageResponse.error != null) {
                        Result.failure(Exception(usageResponse.error))
                    } else {
                        // Mock implementation for development
                        val eventsData = createMockUsageEventsData()
                        Result.success(eventsData)
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to get usage events: ${response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, returning mock usage events")
            Result.success(createMockUsageEventsData())
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun deleteAccount(token: String, confirmPassword: String, reason: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = BillingRequest(
                action = "delete-account",
                confirmPassword = confirmPassword,
                reason = reason
            )
            val response = api.billingAction(authToken, request)
            
            if (response.isSuccessful) {
                response.body()?.let { billingResponse ->
                    if (billingResponse.success) {
                        Result.success(billingResponse.data?.message ?: "Account deleted successfully")
                    } else {
                        Result.failure(Exception(billingResponse.error ?: "Failed to delete account"))
                    }
                } ?: Result.failure(Exception("Empty response"))
            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Authentication failed"))
                    403 -> Result.failure(Exception("Invalid password"))
                    else -> Result.failure(Exception("Failed to delete account: ${response.message()}"))
                }
            }
        } catch (e: java.net.ConnectException) {
            Log.d("BillingRepository", "Backend not available, simulating account deletion")
            // Simulate network delay for development
            delay(2000)
            // Mock implementation - 実際の実装ではAPIを呼び出す
            if (confirmPassword.isNotEmpty()) {
                Result.success("Account deleted successfully")
            } else {
                Result.failure(Exception("Invalid password"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}