import { describe, it, expect } from 'vitest';
import { authClient } from './auth';

describe('Auth Client', () => {
  it('should have required environment variables', () => {
    const requiredVars = authClient.getRequiredEnvVars();
    expect(requiredVars).toContain('NEXTAUTH_URL');
    expect(requiredVars).toContain('NEXTAUTH_SECRET');
  });

  it('should not be authenticated by default (stub)', async () => {
    const isAuthenticated = await authClient.isAuthenticated();
    expect(isAuthenticated).toBe(false);
  });

  it('should validate environment variables', () => {
    // Mock process.env to simulate missing variables
    const originalEnv = process.env;
    process.env = { ...originalEnv };
    
    // Test with missing variables
    delete process.env.NEXTAUTH_URL;
    delete process.env.NEXTAUTH_SECRET;
    
    const result = authClient.validateEnvVars();
    expect(result.valid).toBe(false);
    expect(result.missing).toContain('NEXTAUTH_URL');
    expect(result.missing).toContain('NEXTAUTH_SECRET');
    
    // Restore original env
    process.env = originalEnv;
  });
});