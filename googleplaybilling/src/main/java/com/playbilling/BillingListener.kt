package com.playbilling

interface BillingListener {
    fun onListProductDetails(listProductDetails: List<ProductInfo>) {}
    fun onListPurchase(listPurchases: List<String>) {}
    fun onBillingSetupFailed() {}
    fun onBillingSuccess(billingProduct: ProductInfo?) {}
    fun onUserCancel() {}
    fun onBillingError() {}
    fun onItemAlreadyOwned() {}
}