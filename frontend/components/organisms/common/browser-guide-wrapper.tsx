"use client";

import { LazyBrowserGuideModal } from "@/lib/utils/lazy-components";
import { useBrowserGuide } from "@/hooks";

interface BrowserGuideWrapperProps {
  children: React.ReactNode;
}

export function BrowserGuideWrapper({ children }: BrowserGuideWrapperProps) {
  const { showGuide, hideGuide } = useBrowserGuide();

  return (
    <>
      {children}
      <LazyBrowserGuideModal isOpen={showGuide} onOpenChange={() => hideGuide()} />
    </>
  );
}
