import { Suspense } from "react";
import { AuthHeader, HomeFooter } from "@/components";
import { AdminHeader, AdminClient, LoadingState } from "@/components/organisms/admin";

export default function AdminPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <main className="container mx-auto px-4 py-8">
        <AdminHeader />
        
        <Suspense fallback={<LoadingState />}>
          <AdminClient />
        </Suspense>
      </main>

      <HomeFooter />
    </div>
  );
}
