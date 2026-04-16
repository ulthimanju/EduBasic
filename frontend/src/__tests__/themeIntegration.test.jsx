import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import useThemeMode from '../hooks/useThemeMode';

describe('useThemeMode Integration', () => {
  let mockMatchMedia;

  beforeEach(() => {
    // Setup local storage mock
    let store = {};
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] || null);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
      store[key] = value.toString();
    });

    // Mock matchMedia
    mockMatchMedia = vi.fn().mockImplementation((query) => ({
      matches: false, // default dark
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));
    window.matchMedia = mockMatchMedia;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.documentElement.dataset.theme = '';
    document.documentElement.style.colorScheme = '';
  });

  it('defaults to system and resolves to dark if OS is dark', () => {
    const { result } = renderHook(() => useThemeMode());
    
    expect(result.current.themeMode).toBe('system');
    expect(result.current.effectiveTheme).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('defaults to system and resolves to light if OS prefers light', () => {
    // Change mock to prefer light before hook renders
    mockMatchMedia.mockImplementation((query) => ({
      matches: query === '(prefers-color-scheme: light)', 
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));

    const { result } = renderHook(() => useThemeMode());
    
    expect(result.current.effectiveTheme).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('persisted explicit dark or light overrides OS preference', () => {
    // OS prefers light
    mockMatchMedia.mockImplementation((query) => ({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }));
    
    // User persisted dark
    window.localStorage.setItem('ui-theme-mode', 'dark');

    const { result } = renderHook(() => useThemeMode());
    
    expect(result.current.themeMode).toBe('dark');
    expect(result.current.effectiveTheme).toBe('dark');
  });

  it('changes dataset and storage when setThemeMode is called', () => {
    const { result } = renderHook(() => useThemeMode());
    
    act(() => {
      result.current.setThemeMode('light');
    });

    expect(result.current.themeMode).toBe('light');
    expect(result.current.effectiveTheme).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(document.documentElement.style.colorScheme).toBe('light');
    expect(window.localStorage.getItem('ui-theme-mode')).toBe('light');
  });
});
