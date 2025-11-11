"use client";

import { type ReactNode } from "react";
import { BrowserGuideWrapper } from "@/components/organisms/common/browser-guide-wrapper";
import { KakaoExternalBrowserRedirect } from "@/components/organisms/common/kakao-external-browser-redirect";
import { GlobalToast } from "@/components/molecules/feedback/GlobalToast";

interface ClientProvidersProps {
  children: ReactNode;
}

export function ClientProviders({ children }: ClientProvidersProps) {
  return (
    <>
      <KakaoExternalBrowserRedirect />
      <GlobalToast />
      <BrowserGuideWrapper>{children}</BrowserGuideWrapper>
    </>
  );
}
