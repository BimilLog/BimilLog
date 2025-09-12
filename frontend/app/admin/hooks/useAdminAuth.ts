import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";

export function useAdminAuth() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();
  const isAdmin = user?.role === "ADMIN";

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