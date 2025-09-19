import React from "react";
import Link from "next/link";
import Image from "next/image";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import { Breadcrumb } from "@/components/molecules";
import { AuthLayoutClient } from "./AuthLayoutClient";

interface BreadcrumbItem {
  title: string;
  href?: string;
}

interface AuthLayoutProps {
  children: React.ReactNode;
  title?: string;
  description?: string;
  breadcrumbItems?: BreadcrumbItem[];
}

export const AuthLayout = React.memo<AuthLayoutProps>(({ children, breadcrumbItems }) => {
  return (
    <div className="min-h-screen bg-brand-gradient flex flex-col">
      <AuthHeader />

      {breadcrumbItems && (
        <div className="px-4 pt-4">
          <Breadcrumb items={breadcrumbItems} />
        </div>
      )}

      <div className="flex-1 flex items-center justify-center p-4 py-16">
        <div className="w-full max-w-md">

          {children}

        </div>
      </div>

      <HomeFooter />
      <AuthLayoutClient />
    </div>
  );
});
AuthLayout.displayName = "AuthLayout";