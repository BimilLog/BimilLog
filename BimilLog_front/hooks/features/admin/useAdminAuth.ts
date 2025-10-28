"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";

export function useAdminAuth() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  // 관리자 권한 체크: 인증되지 않았거나 관리자가 아닌 경우 홈으로 리다이렉트
  useEffect(() => {
    if (!isLoading && (!isAuthenticated || !isAdmin)) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, isAdmin, router]);

  return {
    user,
    isAdmin,
    isAuthenticated,
    isLoading,
  };
}