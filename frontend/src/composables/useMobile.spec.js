import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { checkIsMobile } from '@/composables/useMobile';

describe('checkIsMobile', () => {
  const originalMatchMedia = window.matchMedia;

  beforeEach(() => {
    window.matchMedia = vi.fn().mockImplementation((query) => ({
      matches: query.includes('max-width: 430px'),
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    }));
  });

  afterEach(() => {
    window.matchMedia = originalMatchMedia;
  });

  it('uses 430px media query', () => {
    expect(checkIsMobile()).toBe(true);
    expect(window.matchMedia).toHaveBeenCalledWith('(max-width: 430px)');
  });
});
