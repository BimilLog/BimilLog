import { Suspense } from "react";
import { AuthHeader, HomeFooter } from "@/components";
import { AdminHeader } from "@/components/features/admin";
import { AdminClient } from "./components/AdminClient";
import { LoadingState } from "@/components/features/admin";

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
