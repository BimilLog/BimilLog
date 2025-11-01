"use client";

import type { ReactNode } from "react";
import { LazyBrowserGuideModal } from "@/lib/utils/lazy-components";
import { BrowserGuideProvider, useBrowserGuide } from "@/hooks";

interface BrowserGuideWrapperProps {
  children: ReactNode;
}

export function BrowserGuideWrapper({ children }: BrowserGuideWrapperProps) {
  return (
    <BrowserGuideProvider>
      <BrowserGuideWrapperContent>{children}</BrowserGuideWrapperContent>
    </BrowserGuideProvider>
  );
}

function BrowserGuideWrapperContent({ children }: BrowserGuideWrapperProps) {
  const { showGuide, hideGuide } = useBrowserGuide();

  return (
    <>
      {children}
      <LazyBrowserGuideModal
        isOpen={showGuide}
        onOpenChange={() => hideGuide()}
      />
    </>
  );
}
