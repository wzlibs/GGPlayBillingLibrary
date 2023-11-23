package com.playbilling

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType.INAPP
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManager constructor(
    private val context: Context,
    private val subs: List<String>,
    private val inApps: List<String>,
    private val inAppsConsume: List<String>
) {

    private lateinit var billingClient: BillingClient
    private var billingProduct: ProductInfo? = null
    private var listProductDetails = ArrayList<ProductInfo>()
    private var listPurchase = ArrayList<String>()
    private var isConnecting: Boolean = false

    private val billingListeners = ArrayList<BillingListener>()

    fun addBillingListener(billingListener: BillingListener) {
        billingListeners.add(billingListener)

        if (queryPurchaseState == QueryPurchaseState.LOAD_FAILED) {
            billingListener.onBillingSetupFailed()
        }

        if (queryPurchaseState == QueryPurchaseState.LOADED) {
            billingListener.onListPurchase(listPurchase)
        }
        if (queryProductsState == QueryProductsState.LOADED) {
            billingListener.onListProductDetails(listProductDetails)
        }
    }

    fun removeListener(billingListener: BillingListener) {
        billingListeners.remove(billingListener)
    }

    private val purchasesListener = PurchasesUpdatedListener { billingResult, purchases ->
        CoroutineScope(Dispatchers.Main).launch {
            purchasesUpdate(billingResult, purchases)
        }
    }

    private val billingClientStateListener = object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() {
            isConnecting = false
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            isConnecting = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryPurchases()
                queryProducts()
            } else {
                queryProductsState = QueryProductsState.LOAD_FAILED
                queryPurchaseState = QueryPurchaseState.LOAD_FAILED
                CoroutineScope(Dispatchers.Main).launch {
                    billingListeners.forEach { it.onBillingSetupFailed() }
                }
            }
        }
    }

    private enum class QueryPurchaseState {
        LOADING,
        LOADED,
        LOAD_FAILED
    }

    private enum class QueryProductsState {
        LOADING,
        LOADED,
        LOAD_FAILED
    }

    private var queryPurchaseState: QueryPurchaseState = QueryPurchaseState.LOADING
    private var queryProductsState: QueryProductsState = QueryProductsState.LOADING

    init {
        connect()
    }

    private fun connect() {
        if (isConnecting) return
        isConnecting = true
        billingClient =
            BillingClient.newBuilder(context).setListener(purchasesListener)
                .enablePendingPurchases().build()
        billingClient.startConnection(billingClientStateListener)
    }

    fun reconnect() {
        connect()
    }

    private fun purchasesUpdate(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (purchases != null && purchases.isNotEmpty()) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    billingListeners.forEach { it.onBillingSuccess(billingProduct) }
                    billingResponseOk(purchases)
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    billingListeners.forEach { it.onUserCancel() }
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    billingListeners.forEach { it.onItemAlreadyOwned() }
                }

                BillingClient.BillingResponseCode.ERROR -> {
                    billingListeners.forEach { it.onBillingError() }
                }
            }
        }
        billingProduct = null
    }

    private fun billingResponseOk(purchases: List<Purchase>) =
        CoroutineScope(Dispatchers.IO).launch {
            purchases.forEach { purchase ->
                run productsLoop@{
                    purchase.products.forEach { product ->
                        inAppsConsume.find { product == it }?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                val consumeParams =
                                    ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                billingClient.consumePurchase(consumeParams)
                            }
                        }
                        return@productsLoop
                    }
                }
                if (!purchase.isAcknowledged) {
                    Log.d("billing_log", "acknowledgePurchaseParams purchase: $purchase")
                    purchase.products.forEach {
                        Log.d("billing_log", "acknowledgePurchaseParams product: $it")
                    }
                    acknowledgePurchaseParams(
                        billingClient,
                        purchase,
                        object : AcknowledgePurchaseListener {
                            override fun onResponse(success: Boolean) {}
                        })
                }
            }
        }


    fun queryPurchases() = CoroutineScope(Dispatchers.IO).launch {
        queryPurchaseState = QueryPurchaseState.LOADING
        val purchasesProducts = ArrayList<String>()
        queryPurchases(billingClient, INAPP,
            object : QueryPurchaseListener {
                override fun onResponse(products: List<String>) {
                    purchasesProducts.addAll(products)
                    queryPurchases(billingClient, SUBS,
                        object : QueryPurchaseListener {
                            override fun onResponse(products: List<String>) {
                                purchasesProducts.addAll(products)
                                listPurchase = purchasesProducts
                                queryPurchaseState = QueryPurchaseState.LOADED
                                Log.d(
                                    "log_billing123",
                                    "queryPurchases thread: ${Thread.currentThread()}"
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    billingListeners.forEach { it.onListPurchase(purchasesProducts) }
                                }
                            }
                        })
                }
            })
    }

    private fun queryProducts() = CoroutineScope(Dispatchers.IO).launch {
        queryProductsState = QueryProductsState.LOADING
        val productDetailsList = ArrayList<ProductInfo>()
        val listInApp = ArrayList<String>()
        listInApp.addAll(inApps)
        listInApp.addAll(inAppsConsume)
        queryProducts(billingClient, INAPP, listInApp,
            object : QueryProductsListener {
                override fun onResponse(data: List<ProductDetails>) {
                    data.forEach { productDetailsList.add(ProductInfo(it)) }
                    queryProducts(billingClient, SUBS, subs,
                        object : QueryProductsListener {
                            override fun onResponse(data: List<ProductDetails>) {
                                data.forEach { productDetailsList.add(ProductInfo(it)) }
                                listProductDetails = productDetailsList
                                queryProductsState = QueryProductsState.LOADED
                                CoroutineScope(Dispatchers.Main).launch {
                                    billingListeners.forEach {
                                        it.onListProductDetails(productDetailsList)
                                    }
                                }
                            }
                        })
                }
            })
    }

    fun buy(
        activity: Activity,
        productDetails: ProductInfo
    ) {
        billingProduct = productDetails
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails.productDetails)
        if (productDetails.getProductType() == SUBS) {
            val offerTokens = productDetails.getOfferToken()
            if (offerTokens == null) {
                billingProduct = null
                billingListeners.forEach { it.onBillingError() }
                return
            } else {
                productDetailsParams.setOfferToken(offerTokens)
            }
        }
        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams.build())).build()
        )
    }

    fun buy(
        activity: Activity,
        productId: String
    ) {
        val product = listProductDetails.find { it.getProductId() == productId }
        if (product == null) {
            billingListeners.forEach { it.onBillingError() }
        } else {
            buy(activity, product)
        }
    }

    private fun queryProducts(
        billingClient: BillingClient,
        type: String,
        productsId: List<String>,
        queryProductsListener: QueryProductsListener
    ) {
        if (productsId.isEmpty()) {
            queryProductsListener.onResponse(emptyList())
        } else {
            val products = mutableListOf<QueryProductDetailsParams.Product>()
            productsId.forEach {
                products.add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(type)
                        .build()
                )
            }
            val params = QueryProductDetailsParams.newBuilder().setProductList(products)
            billingClient.queryProductDetailsAsync(params.build()) { _, productDetailsList ->
                queryProductsListener.onResponse(productDetailsList)
            }
        }
    }

    private fun queryPurchases(
        billingClient: BillingClient,
        type: String,
        queryPurchaseListener: QueryPurchaseListener
    ) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(type).build()
        ) { billingResult, list ->
            val products = ArrayList<String>()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        products.addAll(purchase.products)
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchaseParams(
                                billingClient,
                                purchase,
                                object : AcknowledgePurchaseListener {
                                    override fun onResponse(success: Boolean) {}
                                })
                        }
                    }
                }
            }
            queryPurchaseListener.onResponse(products)
        }
    }

    private fun acknowledgePurchaseParams(
        billingClient: BillingClient,
        purchase: Purchase,
        acknowledgePurchaseListener: AcknowledgePurchaseListener? = null
    ) = CoroutineScope(Dispatchers.IO).launch {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) {
            Log.d("log_billing123", "acknowledgePurchaseParams thread: ${Thread.currentThread()}")
            CoroutineScope(Dispatchers.Main).launch {
                acknowledgePurchaseListener?.onResponse(it.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

}