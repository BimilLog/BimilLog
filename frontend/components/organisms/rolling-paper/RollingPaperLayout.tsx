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
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />
      {children}
      <HomeFooter />
    </div>
  );
};
