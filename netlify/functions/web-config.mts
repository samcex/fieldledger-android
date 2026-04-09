import type { Config } from "@netlify/functions";

import { json } from "./_shared/http.mts";
import { checkoutConfigured, offers, portalConfigured, webPreviewForced } from "./_shared/stripe.mts";

export default async () => {
  return json({
    forcePro: webPreviewForced(),
    checkoutEnabled: checkoutConfigured(),
    portalEnabled: portalConfigured(),
    offers: offers(),
  });
};

export const config: Config = {
  method: ["GET"],
};
