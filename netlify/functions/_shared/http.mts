function json(data: unknown, init: ResponseInit = {}) {
  const headers = new Headers(init.headers);
  headers.set("content-type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(data), {
    ...init,
    headers,
  });
}

function error(message: string, status = 400, extra: Record<string, unknown> = {}) {
  return json(
    {
      error: message,
      ...extra,
    },
    { status },
  );
}

async function readJson<T>(request: Request): Promise<T | null> {
  const contentType = request.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return null;
  }

  try {
    return (await request.json()) as T;
  } catch (_error) {
    return null;
  }
}

export { error, json, readJson };
