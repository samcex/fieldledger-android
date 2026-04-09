import type { Config } from "@netlify/functions";

import { error, json, readJson } from "./_shared/http.mts";
import { createStripeClient, portalConfigured, siteOrigin } from "./_shared/stripe.mts";

type PortalBody = {
  customerId?: string;
  returnUrl?: string;
};

export default async (request: Request) => {
  if (!portalConfigured()) {
    return error("Stripe billing portal is not configured on this site.", 503);
  }

  const body = await readJson<PortalBody>(request);
  if (!body?.customerId) {
    return error("customerId is required.");
  }

  const stripe = createStripeClient();
  const fallbackReturnUrl = `${siteOrigin(request)}/?tab=Pro`;

  try {
    const session = await stripe.billingPortal.sessions.create({
      customer: body.customerId,
      return_url: body.returnUrl || fallbackReturnUrl,
    });

    return json({
      url: session.url,
    });
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : "Could not create the Stripe billing portal session.";
    return error(message, 500);
  }
};

export const config: Config = {
  method: ["POST"],
};
