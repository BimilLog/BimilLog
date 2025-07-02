"use client";

import { BrowserGuideModal } from "@/components/molecules/browser-guide-modal";

interface BrowserGuideWrapperProps {
  children: React.ReactNode;
}

export function BrowserGuideWrapper({ children }: BrowserGuideWrapperProps) {
  return (
    <>
      {children}
      <BrowserGuideModal />
    </>
  );
}

