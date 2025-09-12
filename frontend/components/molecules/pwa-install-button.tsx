"use client";

import { Button } from "@/components";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";
import { useState } from "react";
import { BrowserGuideModal } from "@/components";

interface PWAInstallButtonProps {
  className?: string;
  variant?: "default" | "outline" | "secondary";
  size?: "sm" | "default" | "lg";
}

export function PWAInstallButton({
  className = "",
  variant = "default",
  size = "default",
}: PWAInstallButtonProps) {
  const { isPWAInstallable, installPWA, getBrowserInfo } = useBrowserGuide();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const browserInfo = getBrowserInfo();
  const isIOS =
    /iPad|iPhone|iPod/.test(navigator.userAgent) && !(window as any).MSStream;

  const handleButtonClick = () => {
    if (isPWAInstallable) {
      installPWA();
    } else {
      setIsModalOpen(true);
    }
  };

  if (browserInfo.isInApp) return null;

  return (
    <>
      <Button
        onClick={handleButtonClick}
        variant={variant}
        size={size}
        className={className}
      >
        {isPWAInstallable
          ? "앱 설치"
          : isIOS
          ? "홈 화면에 추가"
          : "앱 설치 안내"}
      </Button>
      <BrowserGuideModal isOpen={isModalOpen} onOpenChange={setIsModalOpen} />
    </>
  );
}
