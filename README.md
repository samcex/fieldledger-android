# FieldLedger

FieldLedger is an offline-first Android app for solo tradespeople and field-service operators who want a fast way to log jobs, estimate invoice totals, and track payment follow-up without handing their customer data to an ad network.

## What It Does

- Log jobs with customer, service window, labor rate, billed materials, costs, and invoice stage
- See weekly billed totals, estimated profit, and outstanding invoice follow-ups
- Keep an offline history of all logged work
- Guide first-time users through a lightweight onboarding flow
- Support multiple customer-facing currencies from Settings
- Export branded PDF invoices and keep reminder follow-up simple

## Repo Contents

- Kotlin + Jetpack Compose Android app
- Browser-based web app in [`webapp/index.html`](/root/shiftledger-android/webapp/index.html)
- Room persistence for local-first job storage
- Optional Android billing verifier kept in-repo for future experiments in [`backend/README.md`](/root/shiftledger-android/backend/README.md)
- Monetization notes in [`docs/monetization-strategy.md`](/root/shiftledger-android/docs/monetization-strategy.md)
- Play Store copy and asset source files in [`docs/play-store/listing.md`](/root/shiftledger-android/docs/play-store/listing.md)
- Release signing guide in [`docs/release-signing.md`](/root/shiftledger-android/docs/release-signing.md)
- Release notes in [`docs/releases/v0.3.8.md`](/root/shiftledger-android/docs/releases/v0.3.8.md)

## Current MVP

- Dashboard with weekly revenue, costs, profit, and open follow-ups
- First-launch onboarding with default currency selection
- Job entry form with live invoice/profit preview, due date, and reminder fields
- Job pipeline/history screen with invoice export and reminder actions
- Settings screen for currency, branding, and theme preferences
- Responsive web app with matching onboarding, dashboard, job entry, history, and settings flows
- PDF invoice summary export and share flow
- Local unpaid reminder notifications
- Custom logo support on invoice exports in both Android and web
- Release-signed AAB path for Play Console uploads
- Debug APK build for device testing

## Testing Build

The repository can produce both a release-signed `.aab` for Play Console and a debug APK for direct device testing.

Important limits:

- The optional backend is shipped in-repo but not deployed for you
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

To run the web app:

```bash
cd backend
npm install
npm start
```

Then open `http://localhost:8787`.

## Netlify Deploy

The repository now includes [`netlify.toml`](/root/shiftledger-android/netlify.toml) for the web app.

- Publish directory: `webapp`
- SPA routes are redirected to `index.html`
- Netlify Functions expose public Supabase config at `/api/public-config`
- Set `SUPABASE_URL` and `SUPABASE_ANON_KEY` in Netlify if you want Google login, email login, and cloud sync

Outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/bundle/release/app-release.aab
```

## Web Auth And Sync

The web app can stay fully local, or you can turn on account sync with Supabase.

What syncs:

- Google login
- Email and password login
- Jobs in a small `jobs` table
- Company name, currency, and theme in a `profiles` table

What stays local:

- Invoice logo image data

### Supabase Setup

1. Create a Supabase project.
2. In SQL Editor, run [`supabase/schema.sql`](/root/shiftledger-android/supabase/schema.sql).
3. In Auth providers, enable `Google` and add the Google OAuth client ID and secret from Google Cloud.
4. In Auth providers, keep `Email` enabled. On hosted Supabase, email confirmation is enabled by default.
5. In URL Configuration, set:
   - Site URL to your production Netlify URL
   - Redirect URLs to include your production URL and local preview URL
6. Add these Netlify environment variables:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`

Recommended redirect URLs:

- `https://your-site.netlify.app/**`
- `http://localhost:8787/**`

If you use Netlify preview deploys, add the appropriate preview wildcard for that site as well. Supabase documents wildcard redirect URLs for preview environments.

### Local Preview

To test the web app with login locally, add the same public Supabase values to [`backend/.env.example`](/root/shiftledger-android/backend/.env.example) via your local `.env`:

```bash
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
```

Then run:

```bash
cd backend
npm install
npm start
```

Open `http://localhost:8787`, go to Settings, and sign in there.

## Why This Product

The app is intentionally a niche utility, not an ad-first mass-market bet. The goal is to solve a recurring admin pain for tradespeople first and learn whether the workflow becomes sticky before reintroducing pricing decisions.

Reference material:

- Google Play Billing integration: https://developer.android.com/google/play/billing/integrate.html
- Google Play in-app products: https://support.google.com/googleplay/android-developer/answer/1153481?hl=en

## Next Steps

1. Deploy the web app on Netlify and measure activation and repeat usage.
2. Export the branded SVG store assets to final PNGs for Play Console.
3. Add CSV export or email invoice delivery if operators ask for it.
4. Revisit subscriptions later if usage justifies it.
