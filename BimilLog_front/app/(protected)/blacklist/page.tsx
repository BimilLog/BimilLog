"use client";

import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { BlacklistManager } from "@/components/organisms/user/BlacklistManager";

export default function BlacklistPage() {
  return (
    <MainLayout
      className="bg-paper-50"
      containerClassName="container mx-auto px-4"
    >
      <div className="py-8 max-w-4xl mx-auto">
        <BlacklistManager />
      </div>
    </MainLayout>
  );
}
