package com.example.glaceon.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glaceon.data.repository.BillingRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingInfo(
        val customer: Customer? = null,
        val subscription: Subscription? = null,
        val paymentMethods: List<PaymentMethod> = emptyList()
)

data class Customer(
        val userId: String,
        val stripeCustomerId: String?,
        val email: String,
        val name: String,
        val subscriptionStatus: String
)

data class Subscription(
        val id: String,
        val status: String,
        val currentPeriodStart: Long,
        val currentPeriodEnd: Long,
        val cancelAtPeriodEnd: Boolean
)

data class PaymentMethod(val id: String, val type: String, val card: Card?)

data class Card(val brand: String, val last4: String, val expMonth: Int, val expYear: Int)

data class Usage(
        val storageGB: Double,
        val uploadCount: Int,
        val restoreCount: Int,
        val uploadGB: Double = 0.0,
        val restoreGB: Double = 0.0,
        val totalCost: Double,
        val costs: Map<String, Double>
)

data class Invoice(
        val id: String,
        val amountPaid: Int,
        val amountDue: Int,
        val currency: String,
        val status: String,
        val created: Long,
        val periodStart: Long,
        val periodEnd: Long,
        val hostedInvoiceUrl: String?,
        val invoicePdf: String?
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val billingRepository = BillingRepository(application)

    private val _billingInfo = MutableStateFlow<BillingInfo?>(null)
    val billingInfo: StateFlow<BillingInfo?> = _billingInfo.asStateFlow()

    private val _usage = MutableStateFlow<Usage?>(null)
    val usage: StateFlow<Usage?> = _usage.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadBillingInfo(token: String, userEmail: String? = null, userName: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .getBillingInfo(token)
                    .fold(
                            onSuccess = { billingData ->
                                _billingInfo.value =
                                        BillingInfo(
                                                customer =
                                                        billingData.customer?.let { customerData ->
                                                            Customer(
                                                                    userId = customerData.userId,
                                                                    stripeCustomerId =
                                                                            customerData
                                                                                    .stripeCustomerId,
                                                                    email = customerData.email,
                                                                    name = customerData.name,
                                                                    subscriptionStatus =
                                                                            customerData
                                                                                    .subscriptionStatus
                                                            )
                                                        },
                                                subscription =
                                                        billingData.subscription?.let {
                                                                subscriptionData ->
                                                            Subscription(
                                                                    id = subscriptionData.id,
                                                                    status =
                                                                            subscriptionData.status,
                                                                    currentPeriodStart =
                                                                            subscriptionData
                                                                                    .currentPeriodStart *
                                                                                    1000,
                                                                    currentPeriodEnd =
                                                                            subscriptionData
                                                                                    .currentPeriodEnd *
                                                                                    1000,
                                                                    cancelAtPeriodEnd =
                                                                            subscriptionData
                                                                                    .cancelAtPeriodEnd
                                                            )
                                                        },
                                                paymentMethods =
                                                        billingData.paymentMethods?.map {
                                                                paymentMethodData ->
                                                            PaymentMethod(
                                                                    id = paymentMethodData.id,
                                                                    type = paymentMethodData.type,
                                                                    card =
                                                                            paymentMethodData.card
                                                                                    ?.let { cardData
                                                                                        ->
                                                                                        Card(
                                                                                                brand =
                                                                                                        cardData.brand,
                                                                                                last4 =
                                                                                                        cardData.last4,
                                                                                                expMonth =
                                                                                                        cardData.expMonth,
                                                                                                expYear =
                                                                                                        cardData.expYear
                                                                                        )
                                                                                    }
                                                            )
                                                        }
                                                                ?: emptyList()
                                        )
                                _isLoading.value = false
                            },
                            onFailure = { error ->
                                // Customer not foundの場合、自動的に顧客を作成
                                if (error.message?.contains("Customer not found") == true && 
                                    userEmail != null && userName != null) {
                                    setupCustomer(token, userEmail, userName)
                                } else {
                                    _error.value = error.message
                                    _isLoading.value = false
                                }
                            }
                    )
        }
    }

    fun loadUsage(token: String, month: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .getUsage(token, month)
                    .fold(
                            onSuccess = { usageData ->
                                _usage.value =
                                        Usage(
                                                storageGB = usageData.usage.storageGB,
                                                uploadCount = usageData.usage.uploadCount,
                                                restoreCount = usageData.usage.restoreCount,
                                                uploadGB = usageData.usage.uploadGB,
                                                restoreGB = usageData.usage.restoreGB,
                                                totalCost = usageData.totalCost,
                                                costs = usageData.costs
                                        )
                                _isLoading.value = false
                            },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }

    fun loadInvoices(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .getInvoices(token)
                    .fold(
                            onSuccess = { invoiceDataList ->
                                _invoices.value =
                                        invoiceDataList.map { invoiceData ->
                                            Invoice(
                                                    id = invoiceData.id,
                                                    amountPaid = invoiceData.amountPaid,
                                                    amountDue = invoiceData.amountDue,
                                                    currency = invoiceData.currency,
                                                    status = invoiceData.status,
                                                    created = invoiceData.created * 1000,
                                                    periodStart = invoiceData.periodStart * 1000,
                                                    periodEnd = invoiceData.periodEnd * 1000,
                                                    hostedInvoiceUrl = invoiceData.hostedInvoiceUrl,
                                                    invoicePdf = invoiceData.invoicePdf
                                            )
                                        }
                                _isLoading.value = false
                            },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }

    fun setupCustomer(token: String, email: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .setupCustomer(token, email, name)
                    .fold(
                            onSuccess = { customerId -> 
                                loadBillingInfo(token)
                            },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }
    
    fun setupCustomerAndCreateSubscription(token: String, email: String, name: String, priceId: String = "price_1QYqJhJhVGJhVGJh") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // まず顧客を作成
            billingRepository.setupCustomer(token, email, name)
                .fold(
                    onSuccess = { customerId ->
                        // 顧客作成成功後、サブスクリプションを作成
                        billingRepository.createSubscription(token, priceId)
                            .fold(
                                onSuccess = { subscriptionId ->
                                    loadBillingInfo(token)
                                },
                                onFailure = { error ->
                                    _error.value = "Failed to create subscription: ${error.message}"
                                    _isLoading.value = false
                                }
                            )
                    },
                    onFailure = { error ->
                        _error.value = "Failed to setup customer: ${error.message}"
                        _isLoading.value = false
                    }
                )
        }
    }

    fun cancelSubscription(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .cancelSubscription(token)
                    .fold(
                            onSuccess = { message -> loadBillingInfo(token) },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }

    fun addPaymentMethod(token: String, paymentMethodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .addPaymentMethod(token, paymentMethodId)
                    .fold(
                            onSuccess = { message -> loadBillingInfo(token) },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }

    fun createSubscription(token: String, priceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository
                    .createSubscription(token, priceId)
                    .fold(
                            onSuccess = { subscriptionId -> loadBillingInfo(token) },
                            onFailure = { error ->
                                _error.value = error.message
                                _isLoading.value = false
                            }
                    )
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatCurrency(amount: Double): String {
        return String.format("$%.2f", amount)
    }

    fun formatCurrency(amountCents: Int): String {
        return String.format("$%.2f", amountCents / 100.0)
    }

    fun removePaymentMethod(token: String, paymentMethodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository.removePaymentMethod(token, paymentMethodId)
                .fold(
                    onSuccess = { message -> 
                        loadBillingInfo(token)
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        _isLoading.value = false
                    }
                )
        }
    }

    fun getUsageHistory(token: String, months: Int = 12) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository.getUsageHistory(token, months)
                .fold(
                    onSuccess = { historyData ->
                        // 使用量履歴データを処理
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        _isLoading.value = false
                    }
                )
        }
    }

    fun getUsageEvents(token: String, limit: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            billingRepository.getUsageEvents(token, limit)
                .fold(
                    onSuccess = { eventsData ->
                        // 使用量イベントデータを処理
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        _isLoading.value = false
                    }
                )
        }
    }

    fun deleteAccount(token: String, confirmPassword: String, reason: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            billingRepository.deleteAccount(token, confirmPassword, reason)
                .fold(
                    onSuccess = { response ->
                        _isLoading.value = false
                        // アカウント削除成功 - アプリを終了またはログイン画面に戻る
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        _isLoading.value = false
                    }
                )
        }
    }
}