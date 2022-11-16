package com.playbilling

interface AcknowledgePurchaseListener {
    fun onResponse(success: Boolean)
}