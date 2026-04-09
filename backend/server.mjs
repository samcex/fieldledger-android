import dotenv from "dotenv";
import express from "express";
import { GoogleAuth } from "google-auth-library";

dotenv.config();

const app = express();
app.use(express.json());

const androidPublisherScope = "https://www.googleapis.com/auth/androidpublisher";
const allowedPackageName = process.env.PLAY_PACKAGE_NAME;
const credentials = process.env.GOOGLE_SERVICE_ACCOUNT_JSON
  ? JSON.parse(process.env.GOOGLE_SERVICE_ACCOUNT_JSON)
  : undefined;

const auth = new GoogleAuth({
  credentials,
  scopes: [androidPublisherScope],
});

function accessTokenValue(accessToken) {
  if (!accessToken) return null;
  if (typeof accessToken === "string") return accessToken;
  if (typeof accessToken.token === "string") return accessToken.token;
  return null;
}

function latestExpiryTime(lineItems) {
  return lineItems
    .map((item) => item.expiryTime)
    .filter(Boolean)
    .sort()
    .at(-1) ?? null;
}

function matchingLineItems(lineItems, requestedProductIds) {
  if (!Array.isArray(requestedProductIds) || requestedProductIds.length === 0) {
    return lineItems;
  }

  return lineItems.filter((item) => requestedProductIds.includes(item.productId));
}

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.post("/verify/google-play-subscription", async (req, res) => {
  const { packageName, purchaseToken, productIds = [] } = req.body ?? {};

  if (!packageName || !purchaseToken) {
    return res.status(400).json({
      active: false,
      message: "packageName and purchaseToken are required.",
    });
  }

  if (allowedPackageName && packageName !== allowedPackageName) {
    return res.status(403).json({
      active: false,
      message: "packageName is not allowed by this verifier.",
    });
  }

  try {
    const client = await auth.getClient();
    const accessToken = accessTokenValue(await client.getAccessToken());
    if (!accessToken) {
      return res.status(500).json({
        active: false,
        message: "Could not obtain an Android Publisher access token.",
      });
    }

    const response = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: "application/json",
        },
      },
    );

    const payload = await response.json();
    if (!response.ok) {
      return res.status(response.status).json({
        active: false,
        message: payload.error?.message ?? "Google Play verification failed.",
      });
    }

    const lineItems = Array.isArray(payload.lineItems) ? payload.lineItems : [];
    const matchedItems = matchingLineItems(lineItems, productIds);
    const subscriptionState = payload.subscriptionState ?? "";
    const activeStates = new Set([
      "SUBSCRIPTION_STATE_ACTIVE",
      "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
    ]);
    const active = activeStates.has(subscriptionState) && matchedItems.length > 0;

    return res.json({
      active,
      source: "google-play-developer-api",
      subscriptionState,
      latestOrderId: payload.latestOrderId ?? null,
      latestExpiryTime: latestExpiryTime(matchedItems),
      acknowledgementState: payload.acknowledgementState ?? null,
      lineItems: matchedItems,
      testPurchase: Boolean(payload.testPurchase),
      message: active
        ? "Verified active subscription."
        : "Subscription is not active on the backend.",
    });
  } catch (error) {
    return res.status(500).json({
      active: false,
      message: error instanceof Error ? error.message : "Unexpected verifier failure.",
    });
  }
});

const port = Number(process.env.PORT || 8787);
app.listen(port, () => {
  console.log(`FieldLedger billing backend listening on :${port}`);
});
