# WavezBilling

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.pvnam198:WavezBilling:Tag'
	}


billingRepository.addBillingListener(object : BillingListener{
            override fun onListProductDetails(listProductDetails: List<ProductInfo>) {
                super.onListProductDetails(listProductDetails)
            }

            override fun onListPurchase(listPurchases: List<String>) {
                super.onListPurchase(listPurchases)
            }

            override fun onBillingSetupFailed() {
                super.onBillingSetupFailed()
            }

            override fun onBillingSuccess(billingProduct: ProductInfo?) {
                super.onBillingSuccess(billingProduct)
            }

            override fun onUserCancel() {
                super.onUserCancel()
            }

            override fun onBillingError() {
                super.onBillingError()
            }

            override fun onItemAlreadyOwned() {
                super.onItemAlreadyOwned()
            }
        })
