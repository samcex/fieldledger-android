package com.indie.shiftledger.model

object MonetizationPlan {
    const val monthlyProductId = "field_ledger_pro_monthly"
    const val yearlyProductId = "field_ledger_pro_yearly"

    const val freeJobLimit = 15

    val featureBullets = listOf(
        "Unlimited jobs, customers, and invoice stages",
        "Four-week revenue trends and outstanding invoice totals",
        "Invoice export, templates, and follow-up reminders",
    )

    val fallbackOffers = listOf(
        DisplayOffer(
            productId = yearlyProductId,
            title = "Pro Yearly",
            description = "Best value for solo operators who invoice every week",
            price = "$59.99 / year",
        ),
        DisplayOffer(
            productId = monthlyProductId,
            title = "Pro Monthly",
            description = "Lower commitment while testing the workflow",
            price = "$6.99 / month",
        ),
    )
}

data class DisplayOffer(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val offerToken: String? = null,
)
