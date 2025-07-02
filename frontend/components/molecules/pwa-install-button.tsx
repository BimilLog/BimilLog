"use client";

import { Button } from "@/components/atoms/button";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";

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
  const { isPWAInstallable, installPWA } = useBrowserGuide();

  if (!isPWAInstallable) {
    return null;
  }

  return (
    <Button
      onClick={installPWA}
      variant={variant}
      size={size}
      className={className}
    >
      앱 설치
    </Button>
  );
}

