// lib/api-error.ts
// Extracts a human-readable message from a Spring Boot error response body:
// { timestamp, status, error, message, path }

export function getApiErrorMessage(
  err: unknown,
  fallback = "Une erreur est survenue. Veuillez réessayer.",
): string {
  const message = (err as { response?: { data?: { message?: string } } })
    ?.response?.data?.message;
  return message || fallback;
}
