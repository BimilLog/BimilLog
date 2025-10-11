"use client";

import { type ReactNode } from "react";
import { BrowserGuideWrapper } from "@/components/organisms/common/browser-guide-wrapper";
import { GlobalToast } from "@/components/molecules/feedback/GlobalToast";

interface ClientProvidersProps {
  children: ReactNode;
}

export function ClientProviders({ children }: ClientProvidersProps) {
  return (
    <>
      <GlobalToast />
      <BrowserGuideWrapper>{children}</BrowserGuideWrapper>
    </>
  );
}