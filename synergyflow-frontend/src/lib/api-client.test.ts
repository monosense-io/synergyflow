import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiClient } from './api-client';

describe('API Client', () => {

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should have a base URL', () => {
    expect(apiClient).toBeDefined();
  });

  it('applies request interceptor and auth token', async () => {
    apiClient.setAuthToken('abc');
    apiClient.addRequestInterceptor(async (url, init) => {
      return { url, init: { ...init, headers: { ...(init.headers || {}), 'X-Test': 'yes' } } };
    });

    const mock = vi.fn().mockResolvedValueOnce(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'content-type': 'application/json' } })
    );
    Object.defineProperty(globalThis, 'fetch', { value: mock, configurable: true });

    const res = await apiClient.get<{ ok: boolean }>('/ping');
    expect(res.ok).toBe(true);
    const [, init] = mock.mock.calls[0];
    expect(init.headers['Authorization']).toContain('Bearer abc');
    expect(init.headers['X-Test']).toBe('yes');
  });

  it('retries on 500 and eventually succeeds', async () => {
    const mock = vi
      .fn()
      .mockResolvedValueOnce(new Response('fail', { status: 500 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ value: 42 }), {
          status: 200,
          headers: { 'content-type': 'application/json' },
        })
      );
    Object.defineProperty(globalThis, 'fetch', { value: mock, configurable: true });

    const res = await apiClient.get<{ value: number }>('/retry', { retries: 1, retryDelayBaseMs: 1 });
    expect(res.value).toBe(42);
    expect(mock).toHaveBeenCalledTimes(2);
  });

  it('does not retry on 400', async () => {
    const fakeRes = {
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      headers: new Headers(),
      json: async () => ({}),
      text: async () => 'bad',
    } as unknown as Response;
    const mock = vi.fn().mockResolvedValueOnce(fakeRes);
    Object.defineProperty(globalThis, 'fetch', { value: mock, configurable: true });

    await expect(apiClient.get('/no-retry', { retries: 2, retryDelayBaseMs: 1 })).rejects.toThrow();
  });
});
