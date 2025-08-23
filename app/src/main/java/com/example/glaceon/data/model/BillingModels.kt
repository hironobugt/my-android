package com.example.glaceon.data.model

import com.google.gson.annotations.SerializedName

// Request models
data class BillingRequest(
    val action: String,
    val email: String? = null,
    val name: String? = null,
    val paymentMethodId: String? = null,
    val priceId: String? = null,
    val limit: Int? = null,
    val confirmPassword: String? = null,
    val reason: String? = null
)

data class UsageRequest(
    val action: String,
    val month: String? = null
)

// Response models
data class BillingResponse(
    val success: Boolean,
    val data: BillingData? = null,
    val error: String? = null
)

data class BillingData(
    val message: String? = null,
    val customerId: String? = null,
    val customerData: CustomerData? = null,
    val customer: CustomerData? = null,
    val subscription: SubscriptionData? = null,
    val paymentMethods: List<PaymentMethodData>? = null,
    val subscriptionId: String? = null,
    val clientSecret: String? = null,
    val publishableKey: String? = null,
    val status: String? = null,
    val paymentMethodId: String? = null,
    val invoices: List<InvoiceData>? = null,
    val cancelAt: Long? = null
)

data class CustomerData(
    val userId: String,
    val stripeCustomerId: String?,
    val email: String,
    val name: String,
    val createdAt: String? = null,
    val subscriptionStatus: String,
    val billingCycle: String? = null,
    val defaultPaymentMethod: String? = null,
    val subscriptionId: String? = null,
    val updatedAt: String? = null,
    val stripeCustomer: StripeCustomerData? = null
)

data class StripeCustomerData(
    val id: String,
    val email: String,
    val name: String
)

data class SubscriptionData(
    val id: String,
    val status: String,
    @SerializedName("current_period_start")
    val currentPeriodStart: Long,
    @SerializedName("current_period_end")
    val currentPeriodEnd: Long,
    @SerializedName("cancel_at_period_end")
    val cancelAtPeriodEnd: Boolean
)

data class PaymentMethodData(
    val id: String,
    val type: String,
    val card: CardData?
)

data class CardData(
    val brand: String,
    val last4: String,
    @SerializedName("exp_month")
    val expMonth: Int,
    @SerializedName("exp_year")
    val expYear: Int
)

data class InvoiceData(
    val id: String,
    @SerializedName("amount_paid")
    val amountPaid: Int,
    @SerializedName("amount_due")
    val amountDue: Int,
    val currency: String,
    val status: String,
    val created: Long,
    @SerializedName("period_start")
    val periodStart: Long,
    @SerializedName("period_end")
    val periodEnd: Long,
    @SerializedName("hosted_invoice_url")
    val hostedInvoiceUrl: String?,
    @SerializedName("invoice_pdf")
    val invoicePdf: String?
)

data class UsageResponse(
    val usage: UsageInfo? = null,
    val costs: Map<String, Double>? = null,
    val totalCost: Double? = null,
    val pricing: PricingInfo? = null,
    val error: String? = null
)

data class UsageData(
    val usage: UsageInfo,
    val costs: Map<String, Double>,
    val totalCost: Double,
    val pricing: PricingInfo
)

data class UsageInfo(
    val userId: String,
    val periodMonth: String,
    val storageGB: Double? = 0.0,
    val uploadCount: Int? = 0,
    val restoreCount: Int? = 0,
    val uploadGB: Double? = 0.0,
    val restoreGB: Double? = 0.0,
    val apiRequestCount: Int? = 0,
    val thumbnailViews: Int? = 0,
    val lastUpdated: String? = null,
    val totalCost: Double? = 0.0
)

data class PricingInfo(
    val storage: Double,
    val upload: Double,
    val restore: Double,
    val baseFee: Double
)

data class UsageEvent(
    val id: String,
    val userId: String,
    val eventType: String,
    val timestamp: String,
    val eventData: Map<String, Any>,
    val cost: Double = 0.0
)