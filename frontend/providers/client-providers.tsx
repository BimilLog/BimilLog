"use client";

import { type ReactNode } from "react";
import { ToastProvider } from "@/hooks";
import { BrowserGuideWrapper } from "@/components/organisms/common/browser-guide-wrapper";
import { GlobalToast } from "@/components/molecules/feedback/GlobalToast";

interface ClientProvidersProps {
  children: ReactNode;
}

export function ClientProviders({ children }: ClientProvidersProps) {
  return (
    <ToastProvider>
      <GlobalToast />
      <BrowserGuideWrapper>{children}</BrowserGuideWrapper>
    </ToastProvider>
  );
}