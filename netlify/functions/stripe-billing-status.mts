import type { Config } from "@netlify/functions";

import { error, json, readJson } from "./_shared/http.mts";
import { createStripeClient, newestSubscription, serializeSubscription } from "./_shared/stripe.mts";

type BillingStatusBody = {
  customerId?: string;
  installationId?: string;
};

export default async (request: Request) => {
  const body = await readJson<BillingStatusBody>(request);
  if (!body?.customerId) {
    return error("customerId is required.");
  }

  const stripe = createStripeClient();

  try {
    const subscriptions = await stripe.subscriptions.list({
      customer: body.customerId,
      status: "all",
      limit: 20,
    });

    const subscription = newestSubscription(subscriptions, body.installationId || null);
    return json(
      serializeSubscription(
        "stripe-subscription",
        body.customerId,
        subscription,
        body.installationId,
      ),
    );
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : "Could not load Stripe billing status.";
    return error(message, 500);
  }
};

export const config: Config = {
  method: ["POST"],
};
