# FieldLedger Billing Backend

This backend verifies Google Play subscription purchases before the Android app grants `Pro`.

It also serves the browser-based FieldLedger web app from `../webapp`.

## What It Does

- Accepts the app package name, purchase token, and product IDs from the Android client
- Calls Google Play Developer API `purchases.subscriptionsv2.get`
- Returns a compact entitlement response that the app uses to decide whether `Pro` is active

## Local Setup

1. Install Node.js 18 or newer.
2. Copy `.env.example` to `.env`.
3. Provide either:
   - `GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json`, or
   - `GOOGLE_SERVICE_ACCOUNT_JSON={...}`
4. Set `PLAY_PACKAGE_NAME=com.indie.shiftledger`
5. Install and run:

```bash
cd backend
npm install
npm start
```

The server starts on `http://localhost:8787` by default.

## Web App

Open `http://localhost:8787` after the backend starts to load the web app.

Optional preview flags:

- `FIELDLEDGER_WEB_FORCE_PRO=true` unlocks Pro-only web features for internal testing

The production web payment flow now targets Netlify Functions plus Stripe. The local Express server still serves the web app for preview, but Stripe checkout itself is intended to run from the Netlify deployment.

## Android App Configuration

Point the Android client at the backend by setting either:

- Gradle property: `FIELDLEDGER_BILLING_BACKEND_URL`
- Environment variable: `FIELDLEDGER_BILLING_BACKEND_URL`

Example:

```bash
export FIELDLEDGER_BILLING_BACKEND_URL=https://your-domain.example.com
./gradlew assembleRelease
```

## Google Play Setup

The service account used by this backend must have access to the Play Console app and the Android Publisher API enabled in Google Cloud.

Reference docs:

- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate
- Google Play Developer API `purchases.subscriptionsv2.get`: https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/get
