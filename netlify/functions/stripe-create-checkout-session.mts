import type { Config } from "@netlify/functions";

import { error, json, readJson } from "./_shared/http.mts";
import {
  APP_BILLING_NAMESPACE,
  checkoutConfigured,
  createStripeClient,
  resolvePriceId,
  siteOrigin,
} from "./_shared/stripe.mts";

type CheckoutBody = {
  installationId?: string;
  productId?: string;
};

export default async (request: Request) => {
  if (!checkoutConfigured()) {
    return error("Stripe checkout is not configured on this site.", 503);
  }

  const body = await readJson<CheckoutBody>(request);
  if (!body?.installationId || !body?.productId) {
    return error("installationId and productId are required.");
  }

  const priceId = resolvePriceId(body.productId);
  if (!priceId) {
    return error("Unsupported product selection.");
  }

  const stripe = createStripeClient();
  const baseUrl = siteOrigin(request);

  try {
    const session = await stripe.checkout.sessions.create({
      mode: "subscription",
      line_items: [
        {
          price: priceId,
          quantity: 1,
        },
      ],
      client_reference_id: body.installationId,
      metadata: {
        app: APP_BILLING_NAMESPACE,
        installationId: body.installationId,
        productId: body.productId,
      },
      subscription_data: {
        metadata: {
          app: APP_BILLING_NAMESPACE,
          installationId: body.installationId,
          productId: body.productId,
        },
      },
      allow_promotion_codes: true,
      billing_address_collection: "auto",
      success_url: `${baseUrl}/?tab=Pro&checkout=success&session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${baseUrl}/?tab=Pro&checkout=cancel`,
    });

    if (!session.url) {
      return error("Stripe did not return a checkout URL.", 502);
    }

    return json({
      url: session.url,
      sessionId: session.id,
    });
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : "Could not create the Stripe checkout session.";
    return error(message, 500);
  }
};

export const config: Config = {
  method: ["POST"],
};
