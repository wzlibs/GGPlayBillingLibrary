package com.playbilling

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

data class ProductInfo(val productDetails: ProductDetails) {

    fun getProductId(): String {
        return productDetails.productId
    }

    fun getProductType(): String {
        return productDetails.productType
    }

    fun getTitle(): String {
        return productDetails.title
    }

    fun getDescription(): String {
        return productDetails.description
    }

    fun getFormatPrice(): String {
        return if (productDetails.productType == BillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.find { it.priceAmountMicros > 0 }?.formattedPrice.toString()
        } else {
            productDetails.oneTimePurchaseOfferDetails?.formattedPrice.toString()
        }
    }

    fun getPriceCurrencyCode(productDetails: ProductDetails): String {
        return if (productDetails.productType == BillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.find { it.priceAmountMicros > 0 }?.priceCurrencyCode.toString()
        } else {
            productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode.toString()
        }
    }

    fun getOfferToken(): String? {
        return productDetails.subscriptionOfferDetails?.first()?.offerToken
    }

    fun getPriceAmountMicros(): Long {
        return if (productDetails.productType == BillingClient.ProductType.SUBS) {
            productDetails.subscriptionOfferDetails?.first()?.pricingPhases?.pricingPhaseList?.first()?.priceAmountMicros
                ?: 0
        } else {
            productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0
        }
    }

}