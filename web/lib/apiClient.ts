export function apiHeaders(extra?: Record<string, string>): Record<string, string> {
  const secret = process.env.NEXT_PUBLIC_API_SECRET ?? "";
  return { "x-api-secret": secret, ...extra };
}
