# FieldLedger Release Signing

FieldLedger now supports a production signing configuration from Gradle properties or environment variables.

## Required Values

- `FIELDLEDGER_RELEASE_STORE_FILE`
- `FIELDLEDGER_RELEASE_STORE_PASSWORD`
- `FIELDLEDGER_RELEASE_KEY_ALIAS`
- `FIELDLEDGER_RELEASE_KEY_PASSWORD`

You can provide them as:

- environment variables in CI, or
- an untracked local env file such as `.play-signing.env`, or
- entries in an untracked Gradle property source

## Example

```bash
# Option 1: export directly
export FIELDLEDGER_RELEASE_STORE_FILE=/secure/path/fieldledger-release.jks
export FIELDLEDGER_RELEASE_STORE_PASSWORD=change-me
export FIELDLEDGER_RELEASE_KEY_ALIAS=fieldledger
export FIELDLEDGER_RELEASE_KEY_PASSWORD=change-me
export FIELDLEDGER_BILLING_BACKEND_URL=https://billing.example.com

./gradlew bundleRelease

# Option 2: keep the values in an untracked file
cp .play-signing.env.example .play-signing.env
source .play-signing.env
./gradlew bundleRelease
```

## Notes

- If the release signing values are missing, the Gradle `release` build falls back to the debug signing config so local test builds do not fail.
- For Play Console uploads, use a real release keystore and build an `.aab` with `bundleRelease`.
- Keep the keystore and passwords out of git.
