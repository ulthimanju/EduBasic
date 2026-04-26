import { describe, it, expect } from 'vitest';
import { resolveApiBaseUrl } from '../config/runtimeConfig';

describe('runtimeConfig', () => {
  it('falls back to localhost backend when the env var is blank', () => {
    expect(resolveApiBaseUrl('', 'http://localhost:8080')).toBe('http://localhost:8080');
    expect(resolveApiBaseUrl('   ', 'http://localhost:8080')).toBe('http://localhost:8080');
  });

  it('trims whitespace and removes trailing slashes', () => {
    expect(resolveApiBaseUrl(' http://localhost:8080/ ')).toBe('http://localhost:8080');
    expect(resolveApiBaseUrl('https://api.example.com///')).toBe('https://api.example.com');
  });
});
