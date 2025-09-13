"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import { AuthHeader, HomeFooter, Loading } from "@/components";

// Dynamic imports for heavy admin components
const AdminHeader = dynamic(
  () => import("@/components/organisms/admin").then((mod) => ({ default: mod.AdminHeader })),
  {
    ssr: false,
    loading: () => (
      <div className="mb-8 animate-pulse">
        <div className="h-20 bg-gray-200 rounded-lg"></div>
      </div>
    )
  }
);

const AdminClient = dynamic(
  () => import("@/components/organisms/admin").then((mod) => ({ default: mod.AdminClient })),
  {
    ssr: false,
    loading: () => <Loading type="card" message="관리자 대시보드를 불러오는 중..." />
  }
);

export default function AdminPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <main className="container mx-auto px-4 py-8">
        <AdminHeader />

        <Suspense fallback={<Loading type="card" message="관리자 대시보드를 불러오는 중..." />}>
          <AdminClient />
        </Suspense>
      </main>

      <HomeFooter />
    </div>
  );
}
