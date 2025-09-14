"use client";

import { type ReactNode } from "react";
import { ToastProvider } from "@/hooks";
import { BrowserGuideWrapper } from "@/components/organisms/common/browser-guide-wrapper";

interface ClientProvidersProps {
  children: ReactNode;
}

export function ClientProviders({ children }: ClientProvidersProps) {
  return (
    <ToastProvider>
      <BrowserGuideWrapper>{children}</BrowserGuideWrapper>
    </ToastProvider>
  );
}