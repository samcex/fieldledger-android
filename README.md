# FieldLedger

FieldLedger is an offline-first Android app for solo tradespeople and field-service operators who want a fast way to log jobs, estimate invoice totals, and track payment follow-up without handing their customer data to an ad network.

This repo contains:

- A Kotlin + Jetpack Compose Android app scaffold
- Room persistence for locally stored jobs
- Google Play Billing hooks for a `Pro` subscription
- Product and monetization docs in [`docs/monetization-strategy.md`](/root/shiftledger-android/docs/monetization-strategy.md)

## Core MVP

- Log jobs with customer, service window, labor rate, billed materials, costs, and status
- See weekly billed totals, estimated profit, and outstanding invoice follow-ups
- Keep an offline history of all logged work
- Gate premium features behind Google Play subscriptions

## Why this concept

The app is intentionally a niche utility, not an ad-first mass-market bet. Current Android monetization guidance still favors apps with clear recurring value and compliant Play Billing integration over trying to squeeze banner ads into a weak retention product.

Relevant sources:

- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate.html
- Google Play in-app products: https://support.google.com/googleplay/android-developer/answer/1153481?hl=en
- RevenueCat 2026 subscription benchmarks: https://www.revenuecat.com/state-of-subscription-apps-2026-shopping/

## Local status

This machine did not have Java, Kotlin, or Gradle installed when the project was created, so the code has still not been compiled locally here.

## Next steps

1. Open the project in Android Studio.
2. Install the Android SDK for API 36 and JDK 17.
3. Regenerate the Gradle wrapper if needed.
4. Create Play Console subscription products using the IDs in the monetization doc.
5. Replace the MVP client-side entitlement logic with server-side purchase verification before shipping.
