const SUPABASE_MODULE_URL = "https://esm.sh/@supabase/supabase-js@2";

let clientPromise = null;
let clientCacheKey = "";

function hasAuthConfig(config) {
  return Boolean(config?.supabaseUrl && config?.supabaseAnonKey);
}

function nowIso() {
  return new Date().toISOString();
}

function compareIsoTimes(left, right) {
  return String(left ?? "").localeCompare(String(right ?? ""));
}

function randomId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `fieldledger-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function authErrorMessage(error, fallbackMessage) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallbackMessage;
}

async function getAuthClient(config) {
  if (!hasAuthConfig(config)) return null;

  const nextCacheKey = `${config.supabaseUrl}|${config.supabaseAnonKey}`;
  if (!clientPromise || clientCacheKey !== nextCacheKey) {
    clientCacheKey = nextCacheKey;
    clientPromise = import(SUPABASE_MODULE_URL).then(({ createClient }) =>
      createClient(config.supabaseUrl, config.supabaseAnonKey, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: true,
          flowType: "pkce",
        },
      }),
    );
  }

  return clientPromise;
}

async function getCurrentUser(config) {
  const client = await getAuthClient(config);
  if (!client) return null;

  const { data, error } = await client.auth.getUser();
  if (error) {
    throw error;
  }
  return data.user ?? null;
}

async function onAuthChange(config, callback) {
  const client = await getAuthClient(config);
  if (!client) {
    return () => {};
  }

  const {
    data: { subscription },
  } = client.auth.onAuthStateChange((_event, session) => {
    callback(session?.user ?? null);
  });

  return () => subscription.unsubscribe();
}

async function signInWithEmail(config, email, password) {
  const client = await getAuthClient(config);
  if (!client) {
    throw new Error("Supabase auth is not configured.");
  }

  const { error } = await client.auth.signInWithPassword({
    email,
    password,
  });
  if (error) {
    throw error;
  }
}

async function signUpWithEmail(config, email, password, emailRedirectTo = null) {
  const client = await getAuthClient(config);
  if (!client) {
    throw new Error("Supabase auth is not configured.");
  }

  const { data, error } = await client.auth.signUp({
    email,
    password,
    options: emailRedirectTo
      ? {
          emailRedirectTo,
        }
      : undefined,
  });
  if (error) {
    throw error;
  }

  return {
    user: data.user ?? null,
    hasSession: Boolean(data.session),
  };
}

async function signInWithGoogle(config, redirectTo) {
  const client = await getAuthClient(config);
  if (!client) {
    throw new Error("Supabase auth is not configured.");
  }

  const { error } = await client.auth.signInWithOAuth({
    provider: "google",
    options: {
      redirectTo,
    },
  });
  if (error) {
    throw error;
  }
}

async function signOut(config) {
  const client = await getAuthClient(config);
  if (!client) return;

  const { error } = await client.auth.signOut();
  if (error) {
    throw error;
  }
}

async function fetchCloudProfile(config, userId) {
  const client = await getAuthClient(config);
  if (!client || !userId) return null;

  const { data, error } = await client
    .from("profiles")
    .select("user_id, email, company_name, currency_code, theme_mode, updated_at")
    .eq("user_id", userId)
    .maybeSingle();

  if (error) {
    throw error;
  }

  return data ?? null;
}

async function upsertCloudProfile(config, profile) {
  const client = await getAuthClient(config);
  if (!client) {
    throw new Error("Supabase auth is not configured.");
  }

  const { data, error } = await client
    .from("profiles")
    .upsert(
      {
        user_id: profile.userId,
        email: profile.email || null,
        company_name: profile.companyName || "",
        currency_code: profile.currencyCode || "USD",
        theme_mode: profile.themeMode || "light",
        updated_at: nowIso(),
      },
      { onConflict: "user_id" },
    )
    .select("user_id, email, company_name, currency_code, theme_mode, updated_at")
    .single();

  if (error) {
    throw error;
  }

  return data;
}

function serializeJob(job) {
  return {
    clientName: String(job.clientName ?? ""),
    jobName: String(job.jobName ?? ""),
    siteAddress: String(job.siteAddress ?? ""),
    date: String(job.date ?? ""),
    startTime: String(job.startTime ?? ""),
    endTime: String(job.endTime ?? ""),
    laborRate: Number(job.laborRate ?? 0),
    pricingMode: String(job.pricingMode ?? "hourly"),
    fixedPrice: Number(job.fixedPrice ?? 0),
    materialsBilled: Number(job.materialsBilled ?? 0),
    calloutFee: Number(job.calloutFee ?? 0),
    extraCharge: Number(job.extraCharge ?? 0),
    materialsCost: Number(job.materialsCost ?? 0),
    travelCost: Number(job.travelCost ?? 0),
    invoiceStatus: String(job.invoiceStatus ?? "DraftQuote"),
    workSummary: String(job.workSummary ?? ""),
    paymentDueDate: job.paymentDueDate ? String(job.paymentDueDate) : null,
    reminderDate: job.reminderDate ? String(job.reminderDate) : null,
    reminderNote: String(job.reminderNote ?? ""),
  };
}

function deserializeCloudJob(row, localId) {
  const payload = row?.payload && typeof row.payload === "object" ? row.payload : {};
  return {
    id: Number(localId ?? 0),
    cloudId: row?.id ? String(row.id) : null,
    updatedAt: row?.updated_at ? String(row.updated_at) : nowIso(),
    clientName: String(payload.clientName ?? ""),
    jobName: String(payload.jobName ?? ""),
    siteAddress: String(payload.siteAddress ?? ""),
    date: String(payload.date ?? ""),
    startTime: String(payload.startTime ?? "08:00"),
    endTime: String(payload.endTime ?? "12:00"),
    laborRate: Number(payload.laborRate ?? 0),
    pricingMode: String(payload.pricingMode ?? "hourly"),
    fixedPrice: Number(payload.fixedPrice ?? 0),
    materialsBilled: Number(payload.materialsBilled ?? 0),
    calloutFee: Number(payload.calloutFee ?? 0),
    extraCharge: Number(payload.extraCharge ?? 0),
    materialsCost: Number(payload.materialsCost ?? 0),
    travelCost: Number(payload.travelCost ?? 0),
    invoiceStatus: String(payload.invoiceStatus ?? "DraftQuote"),
    workSummary: String(payload.workSummary ?? ""),
    paymentDueDate: payload.paymentDueDate ? String(payload.paymentDueDate) : null,
    reminderDate: payload.reminderDate ? String(payload.reminderDate) : null,
    reminderNote: String(payload.reminderNote ?? ""),
  };
}

async function listCloudJobs(config, userId) {
  const client = await getAuthClient(config);
  if (!client || !userId) return [];

  const { data, error } = await client
    .from("jobs")
    .select("id, updated_at, payload")
    .eq("user_id", userId)
    .order("updated_at", { ascending: false });

  if (error) {
    throw error;
  }

  return data ?? [];
}

async function upsertCloudJob(config, userId, job) {
  const client = await getAuthClient(config);
  if (!client) {
    throw new Error("Supabase auth is not configured.");
  }

  const { data, error } = await client
    .from("jobs")
    .upsert(
      {
        id: job.cloudId || randomId(),
        user_id: userId,
        updated_at: job.updatedAt || nowIso(),
        payload: serializeJob(job),
      },
      { onConflict: "id" },
    )
    .select("id, updated_at, payload")
    .single();

  if (error) {
    throw error;
  }

  return data;
}

async function deleteCloudJob(config, userId, cloudId) {
  const client = await getAuthClient(config);
  if (!client || !userId || !cloudId) return;

  const { error } = await client
    .from("jobs")
    .delete()
    .eq("user_id", userId)
    .eq("id", cloudId);

  if (error) {
    throw error;
  }
}

export {
  authErrorMessage,
  compareIsoTimes,
  deserializeCloudJob,
  deleteCloudJob,
  fetchCloudProfile,
  getCurrentUser,
  hasAuthConfig,
  listCloudJobs,
  onAuthChange,
  randomId,
  signInWithEmail,
  signInWithGoogle,
  signOut,
  signUpWithEmail,
  upsertCloudJob,
  upsertCloudProfile,
};
