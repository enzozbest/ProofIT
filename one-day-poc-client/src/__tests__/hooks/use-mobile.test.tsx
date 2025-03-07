import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import * as React from 'react';
import { useIsMobile } from '../../hooks/use-mobile';
import '../mocks/use-mobile.mock';

const TestComponent = () => {
  const isMobile = useIsMobile();
  return <div data-testid="is-mobile">{isMobile ? 'Mobile' : 'Desktop'}</div>;
};

describe('useIsMobile', () => {
  let originalInnerWidth: number;
  let listeners: { [key: string]: (event: Event) => void } = {};

  beforeEach(() => {
    originalInnerWidth = window.innerWidth;
    vi.spyOn(window, 'matchMedia').mockImplementation((query) => {
      return {
        matches: window.innerWidth < 768,
        media: query,
        onchange: null,
        addEventListener: (event: string, listener: (event: Event) => void) => {
          listeners[event] = listener;
        },
        removeEventListener: (event: string) => {
          delete listeners[event];
        },
        dispatchEvent: (event: Event) => {
          if (listeners[event.type]) {
            listeners[event.type](event);
          }
        },
      };
    });
  });

  afterEach(() => {
    window.innerWidth = originalInnerWidth;
    vi.resetAllMocks();
    listeners = {};
  });

  test('returns true when window width is < MOBILE_BREAKPOINT', () => {
    window.innerWidth = 100;
    render(<TestComponent />);
    expect(screen.getByTestId('is-mobile').textContent).toBe('Mobile');
  });

  test('returns false when window width is >= MOBILE_BREAKPOINT', () => {
    window.innerWidth = 1000;
    render(<TestComponent />);
    expect(screen.getByTestId('is-mobile').textContent).toBe('Desktop');
  });

  test('setIsMobile function updates state correctly', () => {
    const { rerender } = render(<TestComponent />);
    const mql = window.matchMedia(`(max-width: 767px)`);

    act(() => {
      window.innerWidth = 100;
      listeners['change'](new Event('change'));
    });
    rerender(<TestComponent />);
    expect(screen.getByTestId('is-mobile').textContent).toBe('Mobile');

    act(() => {
      window.innerWidth = 1000;
      listeners['change'](new Event('change'));
    });
    rerender(<TestComponent />);
    expect(screen.getByTestId('is-mobile').textContent).toBe('Desktop');
  });
});
