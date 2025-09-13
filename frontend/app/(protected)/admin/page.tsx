import { Suspense } from "react";
import { AuthHeader, HomeFooter, Loading } from "@/components";
import { AdminHeader, AdminClient } from "@/components/organisms/admin";

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
