import React from 'react';

const MOBILE_BREAKPOINT = 768

/**
 * Custom hook that detects whether the current viewport width is in mobile range
 * 
 * Uses the browser's matchMedia API to listen for viewport size changes and
 * updates the returned value automatically when the screen size crosses the
 * mobile breakpoint threshold.
 * 
 * @returns {boolean} True if the current viewport width is below the mobile breakpoint,
 *                    false otherwise
 * 
 * @example
 * ```tsx
 * function ResponsiveComponent() {
 *   const isMobile = useIsMobile();
 *   
 *   return (
 *     <div>
 *       {isMobile ? <MobileView /> : <DesktopView />}
 *     </div>
 *   );
 * }
 * ```
 */
export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState<boolean | undefined>(undefined)

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`)
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    }
    mql.addEventListener("change", onChange)
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    return () => mql.removeEventListener("change", onChange)
  }, [])

  return !!isMobile
}
