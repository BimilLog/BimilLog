"use client";

import React from "react";
import { Button } from "./button";

interface BackButtonProps extends React.ComponentProps<typeof Button> {
  fallbackHref?: string;
}

export function BackButton({
  fallbackHref = "/",
  children,
  onClick,
  ...props
}: BackButtonProps) {
  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (onClick) {
      onClick(e);
      return;
    }

    // 브라우저 히스토리가 있으면 뒤로 가기, 없으면 fallback
    if (typeof window !== "undefined") {
      if (window.history.length > 1) {
        window.history.back();
      } else {
        window.location.href = fallbackHref;
      }
    }
  };

  return (
    <Button onClick={handleClick} {...props}>
      {children}
    </Button>
  );
}