package com.playbilling

import com.android.billingclient.api.ProductDetails

interface QueryProductsListener {
    fun onResponse(data: List<ProductDetails>)
}