import { config } from "@/config/config";
import { useCookie } from "@/hooks/useCookie";

export class ApiError extends Error {
  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = "ApiError: " + status;
  }
}

interface FetchOptions<B> {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: B;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

async function request<T, B = unknown, P = unknown>(endpoint: string, options: FetchOptions<B> = {}, params?: P): Promise<T> {
  const cookie = useCookie();
  const token = cookie.getCookie(config.tokenCookieNome)
  const { method = "GET", body, headers = {}, signal } = options;

  const data = method !== "GET" && body ? JSON.stringify(body) : undefined

  const response = await fetch(`${config.apiUrl}${endpoint}${params ? `?${new URLSearchParams(params as Record<string, string>).toString()}` : ""}`, {
    method,
    signal,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: data,
  });

  if (!response.ok) {
    throw new ApiError(`Requisição falhou: ${response.statusText}`, response.status);
  }

  return response.json() as Promise<T>;
}

export const httpClient = {
  get: <T, P = unknown>(endpoint: string, params?: P, signal?: AbortSignal) => request<T, unknown, P>(endpoint, { method: "GET", signal }, params),
  post: <T, B>(endpoint: string, body: B) => request<T, B>(endpoint, { method: "POST", body }),
  put: <T, B>(endpoint: string, body: B) => request<T, B>(endpoint, { method: "PUT", body }),
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: "DELETE" }),
  patch: <T, B>(endpoint: string, body: B) => request<T, B>(endpoint, { method: "PATCH", body }),
};
