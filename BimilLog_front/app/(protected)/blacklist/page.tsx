"use client";

import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { BlacklistManager } from "@/components/organisms/user/BlacklistManager";

export default function BlacklistPage() {
  return (
    <MainLayout
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
      containerClassName="container mx-auto px-4"
    >
      <div className="py-8 max-w-4xl mx-auto">
        <BlacklistManager />
      </div>
    </MainLayout>
  );
}
