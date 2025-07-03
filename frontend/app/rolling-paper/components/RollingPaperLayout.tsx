"use client";

import React from "react";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

interface RollingPaperLayoutProps {
  children: React.ReactNode;
  adPosition: string;
}

export const RollingPaperLayout: React.FC<RollingPaperLayoutProps> = ({
  children,
  adPosition,
}) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />
      {children}
      <HomeFooter />
    </div>
  );
};
