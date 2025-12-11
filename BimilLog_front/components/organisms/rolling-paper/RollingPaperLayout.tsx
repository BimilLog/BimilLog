import React from "react";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

interface RollingPaperLayoutProps {
  children: React.ReactNode;
}

export const RollingPaperLayout: React.FC<RollingPaperLayoutProps> = ({
  children,
}) => {
  return (
    <div className="min-h-screen bg-brand-gradient">
      {/* Auth Header */}
      <AuthHeader />
      {children}
      <HomeFooter />
    </div>
  );
};
