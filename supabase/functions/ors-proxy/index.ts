// Supabase Edge Function: proxies OpenRouteService so the ORS key never ships on-device.
// Request body: { "endpoint": "directions" | "matrix", "profile": "foot-walking" | "cycling-regular" | "driving-car", "body": <ORS request payload> }
// Auth: default JWT verification applies (caller must send the Supabase anon or user Authorization header).

const ORS_BASE_URL = "https://api.openrouteservice.org/v2";
const ALLOWED_ENDPOINTS = new Set(["directions", "matrix"]);
const ALLOWED_PROFILES = new Set(["foot-walking", "cycling-regular", "driving-car"]);

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Use POST" }), {
      status: 405,
      headers: { "Content-Type": "application/json" },
    });
  }

  const orsApiKey = Deno.env.get("ORS_API_KEY");
  if (!orsApiKey) {
    return new Response(JSON.stringify({ error: "ORS_API_KEY not configured" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  let payload: { endpoint?: string; profile?: string; body?: unknown };
  try {
    payload = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }

  const { endpoint, profile, body } = payload;
  if (!endpoint || !ALLOWED_ENDPOINTS.has(endpoint)) {
    return new Response(JSON.stringify({ error: "endpoint must be 'directions' or 'matrix'" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }
  if (!profile || !ALLOWED_PROFILES.has(profile)) {
    return new Response(
      JSON.stringify({ error: "profile must be one of foot-walking, cycling-regular, driving-car" }),
      { status: 400, headers: { "Content-Type": "application/json" } },
    );
  }

  const orsResponse = await fetch(`${ORS_BASE_URL}/${endpoint}/${profile}`, {
    method: "POST",
    headers: {
      "Authorization": orsApiKey,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body ?? {}),
  });

  const responseText = await orsResponse.text();
  return new Response(responseText, {
    status: orsResponse.status,
    headers: { "Content-Type": "application/json" },
  });
});
