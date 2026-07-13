import axios from 'axios';

import type { ApiError } from '@/types/api';

/**
 * Turns any thrown value from an Axios call into a human-readable message,
 * preferring the backend's consistent ApiError body.
 */
export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (error.code === 'ECONNABORTED') return 'The request timed out. Check your connection.';
    if (!error.response) return 'Cannot reach the server. Check your connection and API URL.';

    const body = error.response.data;
    if (body?.fieldErrors?.length) {
      return body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('\n');
    }
    if (body?.message) return body.message;
    return `Request failed (${error.response.status}).`;
  }
  if (error instanceof Error) return error.message || fallback;
  return fallback;
}

/** Field-level errors, keyed by field name, for wiring into react-hook-form. */
export function getApiFieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.fieldErrors) {
    return Object.fromEntries(
      error.response.data.fieldErrors.map((f) => [f.field, f.message]),
    );
  }
  return {};
}
