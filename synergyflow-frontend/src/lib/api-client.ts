// API Client with interceptors and retry/backoff template

export type RequestInterceptor = (
  url: string,
  init: RequestInit
) => Promise<{ url: string; init: RequestInit }> | { url: string; init: RequestInit };

export type ResponseInterceptor = (
  response: Response,
  request: { url: string; init: RequestInit }
) => Promise<Response> | Response;

export interface RequestOptions extends RequestInit {
  retries?: number; // number of retries on failure
  retryDelayBaseMs?: number; // base delay for backoff
  retryOn?: (resOrErr: Response | Error) => boolean; // predicate to retry
}

class ApiClient {
  private baseUrl: string;
  private defaultHeaders: Record<string, string>;
  private authToken: string | null = null;
  private requestInterceptors: RequestInterceptor[] = [];
  private responseInterceptors: ResponseInterceptor[] = [];

  constructor() {
    this.baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
    this.defaultHeaders = {
      'Content-Type': 'application/json',
    };
  }

  public setAuthToken(token: string | null) {
    this.authToken = token;
  }

  public addRequestInterceptor(interceptor: RequestInterceptor) {
    this.requestInterceptors.push(interceptor);
  }

  public addResponseInterceptor(interceptor: ResponseInterceptor) {
    this.responseInterceptors.push(interceptor);
  }

  private async applyRequestInterceptors(url: string, init: RequestInit) {
    let current = { url, init };
    for (const interceptor of this.requestInterceptors) {
      current = await interceptor(current.url, current.init);
    }
    return current;
  }

  private async applyResponseInterceptors(response: Response, req: { url: string; init: RequestInit }) {
    let current: Response = response;
    for (const interceptor of this.responseInterceptors) {
      const next = await interceptor(current, req);
      current = next;
    }
    return current;
  }

  private async delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private shouldRetryDefault(resOrErr: Response | Error) {
    if (resOrErr instanceof Error) return true; // network errors
    return resOrErr.status >= 500 && resOrErr.status < 600; // 5xx
  }

  private async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const headers: Record<string, string> = {
      ...this.defaultHeaders,
      ...(options.headers as Record<string, string> | undefined),
    };
    if (this.authToken && !headers['Authorization']) {
      headers['Authorization'] = `Bearer ${this.authToken}`;
    }
    const init: RequestInit = { ...options, headers };

    // Apply request interceptors
    const req = await this.applyRequestInterceptors(url, init);

    const retries = options.retries ?? 2;
    const base = options.retryDelayBaseMs ?? 200;
    const retryOn = options.retryOn ?? this.shouldRetryDefault.bind(this);

    let attempt = 0;
    // eslint-disable-next-line no-constant-condition
    while (true) {
      try {
        const response = await fetch(req.url, { ...req.init });
        // Handle HTTP error status before running response interceptors
        if (!response.ok) {
          if (retryOn(response) && attempt < retries) {
            await this.delay(Math.pow(2, attempt) * base);
            attempt++;
            continue;
          }
          throw new Error(`API request failed: ${response.status} ${response.statusText}`);
        }

        // Now allow response interceptors for successful responses
        const processed = await this.applyResponseInterceptors(response, req);
        const ct = processed.headers.get('content-type') || '';
        if (processed.status === 204) return undefined as unknown as T;
        if (ct.includes('application/json')) {
          return (await processed.json()) as T;
        }
        // Fallback to text
        return (await processed.text()) as unknown as T;
      } catch (err) {
        if (retryOn(err as Error) && attempt < retries) {
          await this.delay(Math.pow(2, attempt) * base);
          attempt++;
          continue;
        }
        // eslint-disable-next-line no-console
        console.error('API request error:', err);
        throw err;
      }
    }
  }

  public get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'GET' });
  }

  public post<T>(endpoint: string, data: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  public put<T>(endpoint: string, data: unknown, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  public delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
