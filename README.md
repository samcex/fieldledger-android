# FieldLedger

FieldLedger is an offline-first Android app for solo tradespeople and field-service operators who want a fast way to log jobs, estimate invoice totals, and track payment follow-up without handing their customer data to an ad network.

## What It Does

- Log jobs with customer, service window, labor rate, billed materials, costs, and invoice stage
- See weekly billed totals, estimated profit, and outstanding invoice follow-ups
- Keep an offline history of all logged work
- Guide first-time users through a lightweight onboarding flow
- Support multiple customer-facing currencies from Settings
- Prepare a monetization path through Google Play subscriptions

## Repo Contents

- Kotlin + Jetpack Compose Android app
- Room persistence for local-first job storage
- Google Play Billing integration with backend-verification client
- Node backend verifier in [`backend/README.md`](/root/shiftledger-android/backend/README.md)
- Monetization notes in [`docs/monetization-strategy.md`](/root/shiftledger-android/docs/monetization-strategy.md)
- Play Store copy and asset source files in [`docs/play-store/listing.md`](/root/shiftledger-android/docs/play-store/listing.md)
- Release signing guide in [`docs/release-signing.md`](/root/shiftledger-android/docs/release-signing.md)
- Release notes in [`docs/releases/v0.3.2.md`](/root/shiftledger-android/docs/releases/v0.3.2.md)

## Current MVP

- Dashboard with weekly revenue, costs, profit, and open follow-ups
- First-launch onboarding with default currency selection
- Job entry form with live invoice/profit preview, due date, and reminder fields
- Job pipeline/history screen with invoice export and reminder actions
- Settings screen for currency preferences
- PDF invoice summary export and share flow
- Local unpaid reminder notifications
- Google Play subscription paywall wired for backend verification
- Release-signed AAB path for Play Console uploads
- Debug APK build for device testing

## Testing Build

The repository can produce both a release-signed `.aab` for Play Console and a debug APK for direct device testing.

Important limits:

- Billing requires a configured backend verifier URL for real Pro unlocks
- The included backend is shipped in-repo but not deployed for you
- Local signing material must stay private and out of git

## Build Requirements

- JDK 17
- Android SDK Platform 36
- Android Build Tools 35.0.0
- Gradle 8.13
- Node.js 18+ if you want to run the bundled billing backend locally

## Local Build

```bash
./gradlew assembleDebug
./gradlew bundleRelease
```

Outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/bundle/release/app-release.aab
```

## Why This Product

The app is intentionally a niche utility, not an ad-first mass-market bet. The goal is to solve a recurring admin pain for tradespeople first, then monetize the workflow once it becomes sticky.

Reference material:

- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate.html
- Google Play in-app products: https://support.google.com/googleplay/android-developer/answer/1153481?hl=en
- RevenueCat 2026 subscription benchmarks: https://www.revenuecat.com/state-of-subscription-apps-2026-shopping/

## Next Steps

1. Deploy the bundled billing backend and connect it to a real Play Console app.
2. Export the branded SVG store assets to final PNGs for Play Console.
3. Add edit/update flows for saved jobs and reminders.
4. Add CSV export or email invoice delivery if you want a stronger upsell than static PDF share.
