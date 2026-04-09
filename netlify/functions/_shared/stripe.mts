import Stripe from "stripe";

const APP_BILLING_NAMESPACE = "fieldledger-web";
const ACTIVE_SUBSCRIPTION_STATUSES = new Set(["active", "trialing", "past_due"]);

function env(name: string): string | undefined {
  const netlifyValue = globalThis.Netlify?.env?.get?.(name);
  if (netlifyValue) return netlifyValue;
  const processValue = process.env[name];
  return processValue && processValue.length > 0 ? processValue : undefined;
}

function requireEnv(name: string): string {
  const value = env(name);
  if (!value) {
    throw new Error(`Missing required environment variable ${name}.`);
  }
  return value;
}

function createStripeClient() {
  return new Stripe(requireEnv("STRIPE_SECRET_KEY"));
}

function checkoutConfigured() {
  return Boolean(env("STRIPE_SECRET_KEY") && env("STRIPE_MONTHLY_PRICE_ID") && env("STRIPE_YEARLY_PRICE_ID"));
}

function portalConfigured() {
  return Boolean(env("STRIPE_SECRET_KEY"));
}

function webPreviewForced() {
  return ["1", "true", "yes"].includes(String(env("FIELDLEDGER_WEB_FORCE_PRO") || "").toLowerCase());
}

function offers() {
  return [
    {
      productId: "field_ledger_pro_yearly",
      title: "Pro Yearly",
      description: "Best value for solo operators who invoice every week",
      price: "$59.99 / year",
      checkoutEnabled: checkoutConfigured(),
    },
    {
      productId: "field_ledger_pro_monthly",
      title: "Pro Monthly",
      description: "Lower commitment while testing the workflow",
      price: "$6.99 / month",
      checkoutEnabled: checkoutConfigured(),
    },
  ];
}

function resolvePriceId(productId: string): string | null {
  if (productId === "field_ledger_pro_monthly") {
    return env("STRIPE_MONTHLY_PRICE_ID") || null;
  }
  if (productId === "field_ledger_pro_yearly") {
    return env("STRIPE_YEARLY_PRICE_ID") || null;
  }
  return null;
}

function siteOrigin(request: Request) {
  const requestOrigin = request.headers.get("origin");
  if (requestOrigin) return requestOrigin;

  const configuredSiteUrl = env("URL");
  if (configuredSiteUrl) return configuredSiteUrl;

  return new URL(request.url).origin;
}

function activeSubscription(subscription: Stripe.Subscription | null, installationId?: string | null) {
  if (!subscription) return false;
  if (!ACTIVE_SUBSCRIPTION_STATUSES.has(subscription.status)) return false;
  if (!installationId) return true;
  return subscription.metadata.installationId === installationId;
}

function serializeSubscription(
  source: string,
  customerId: string | null,
  subscription: Stripe.Subscription | null,
  installationId?: string | null,
) {
  return {
    active: activeSubscription(subscription, installationId),
    source,
    customerId,
    subscriptionId: subscription?.id ?? null,
    subscriptionStatus: subscription?.status ?? null,
    currentPeriodEnd: subscription?.current_period_end
      ? new Date(subscription.current_period_end * 1000).toISOString()
      : null,
    message: subscription
      ? ACTIVE_SUBSCRIPTION_STATUSES.has(subscription.status)
        ? "Pro verified by Stripe."
        : `Stripe subscription is ${subscription.status}.`
      : "No Stripe subscription was found for this device.",
  };
}

function newestSubscription(subscriptions: Stripe.ApiList<Stripe.Subscription>, installationId?: string | null) {
  const matching = subscriptions.data
    .filter((subscription) => {
      if (subscription.metadata.app !== APP_BILLING_NAMESPACE) return false;
      if (!installationId) return true;
      return subscription.metadata.installationId === installationId;
    })
    .sort((left, right) => (right.current_period_end || 0) - (left.current_period_end || 0));

  return (
    matching.find((subscription) => ACTIVE_SUBSCRIPTION_STATUSES.has(subscription.status)) ??
    matching[0] ??
    null
  );
}

export {
  APP_BILLING_NAMESPACE,
  ACTIVE_SUBSCRIPTION_STATUSES,
  checkoutConfigured,
  createStripeClient,
  newestSubscription,
  offers,
  portalConfigured,
  resolvePriceId,
  serializeSubscription,
  siteOrigin,
  webPreviewForced,
};
