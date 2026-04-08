# FieldLedger

FieldLedger is an offline-first Android app for solo tradespeople and field-service operators who want a fast way to log jobs, estimate invoice totals, and track payment follow-up without handing their customer data to an ad network.

## What It Does

- Log jobs with customer, service window, labor rate, billed materials, costs, and invoice stage
- See weekly billed totals, estimated profit, and outstanding invoice follow-ups
- Keep an offline history of all logged work
- Prepare a monetization path through Google Play subscriptions

## Repo Contents

- Kotlin + Jetpack Compose Android app
- Room persistence for local-first job storage
- Google Play Billing integration scaffold for `Pro`
- Monetization notes in [`docs/monetization-strategy.md`](/root/shiftledger-android/docs/monetization-strategy.md)
- Release notes in [`docs/releases/v0.1.0.md`](/root/shiftledger-android/docs/releases/v0.1.0.md)

## Current MVP

- Dashboard with weekly revenue, costs, profit, and open follow-ups
- Job entry form with live invoice/profit preview
- Job pipeline/history screen with invoice stage badges
- Google Play subscription paywall shell
- Debug APK build for device testing

## Testing Build

The repository currently ships a debug APK for testing through GitHub Releases. It is not a Play-ready production build.

Important limits:

- The current release is debug-signed
- Billing is client-side MVP logic only
- No backend purchase verification yet
- No invoice PDF export yet

## Build Requirements

- JDK 17
- Android SDK Platform 36
- Android Build Tools 35.0.0
- Gradle 8.13

## Local Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Why This Product

The app is intentionally a niche utility, not an ad-first mass-market bet. The goal is to solve a recurring admin pain for tradespeople first, then monetize the workflow once it becomes sticky.

Reference material:

- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate.html
- Google Play in-app products: https://support.google.com/googleplay/android-developer/answer/1153481?hl=en
- RevenueCat 2026 subscription benchmarks: https://www.revenuecat.com/state-of-subscription-apps-2026-shopping/

## Next Steps

1. Add invoice/PDF export.
2. Replace client-side billing state with server-side purchase verification.
3. Add reminder workflows for unpaid jobs.
4. Create release and Play signing configs for production.
