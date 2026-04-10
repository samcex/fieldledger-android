export default async () => {
  return Response.json({
    supabaseUrl: Netlify.env.get("SUPABASE_URL") || null,
    supabaseAnonKey: Netlify.env.get("SUPABASE_ANON_KEY") || null,
  });
};

export const config = {
  path: "/api/public-config",
};
