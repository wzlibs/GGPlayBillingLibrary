package com.playbilling

interface QueryPurchaseListener {
    fun onResponse(products: List<String>)
}