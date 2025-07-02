"use client";

import { BrowserGuideModal } from "@/components/molecules/browser-guide-modal";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";

interface BrowserGuideWrapperProps {
  children: React.ReactNode;
}

export function BrowserGuideWrapper({ children }: BrowserGuideWrapperProps) {
  const { showGuide, hideGuide } = useBrowserGuide();

  return (
    <>
      {children}
      <BrowserGuideModal isOpen={showGuide} onOpenChange={() => hideGuide()} />
    </>
  );
}
