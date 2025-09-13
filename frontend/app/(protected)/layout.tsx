"use client";

import { useEffect, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { AuthHeader } from "@/components/layouts";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { LoadingSpinner } from "@/components/atoms";
import type { AuthLayoutProps } from "@/types/auth";

export default function AuthenticatedLayout({ children }: AuthLayoutProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  const shouldShowLoading = isLoading;
  const shouldShowContent = !isLoading && isAuthenticated;

  const loadingScreen = useMemo(() => (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <LoadingSpinner
        variant="gradient"
        message="로그인 상태를 확인하는 중..."
      />
    </div>
  ), []);

  if (shouldShowLoading) {
    return loadingScreen;
  }

  if (!shouldShowContent) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />
      <main>{children}</main>
      <HomeFooter />
    </div>
  );
}