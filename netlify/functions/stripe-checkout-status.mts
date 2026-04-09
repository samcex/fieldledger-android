import type { Config } from "@netlify/functions";
import type Stripe from "stripe";

import { error, json, readJson } from "./_shared/http.mts";
import { createStripeClient, serializeSubscription } from "./_shared/stripe.mts";

type CheckoutStatusBody = {
  installationId?: string;
  sessionId?: string;
};

export default async (request: Request) => {
  const body = await readJson<CheckoutStatusBody>(request);
  if (!body?.sessionId || !body?.installationId) {
    return error("sessionId and installationId are required.");
  }

  const stripe = createStripeClient();

  try {
    const session = await stripe.checkout.sessions.retrieve(body.sessionId, {
      expand: ["subscription"],
    });

    if (session.client_reference_id !== body.installationId) {
      return error("Checkout session does not belong to this installation.", 403);
    }

    const customerId =
      typeof session.customer === "string"
        ? session.customer
        : session.customer?.id ?? null;

    let subscription: Stripe.Subscription | null = null;
    if (session.subscription && typeof session.subscription !== "string") {
      subscription = session.subscription;
    } else if (typeof session.subscription === "string") {
      subscription = await stripe.subscriptions.retrieve(session.subscription);
    }

    return json(
      serializeSubscription(
        "stripe-checkout-session",
        customerId,
        subscription,
        body.installationId,
      ),
    );
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : "Could not verify the Stripe checkout session.";
    return error(message, 500);
  }
};

export const config: Config = {
  method: ["POST"],
};
