"use client";

import { useEffect, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { LoadingSpinner } from "@/components/atoms";
import type { AuthLayoutProps } from "@/types/domains/auth";

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
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]">
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

  return <>{children}</>;
}