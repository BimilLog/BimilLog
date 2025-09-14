import React from "react";
import Link from "next/link";
import Image from "next/image";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import { AuthLayoutClient } from "./AuthLayoutClient";

interface AuthLayoutProps {
  children: React.ReactNode;
  title?: string;
  description?: string;
}

export const AuthLayout = React.memo<AuthLayoutProps>(({ children }) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />
      
      <div className="flex items-center justify-center p-4 py-16">
        <div className="w-full max-w-md">
          {/* Logo */}
          <div className="text-center mb-8">
            <Link href="/" className="inline-block">
              <Image
                src="/log.png"
                alt="비밀로그"
                width={64}
                height={64}
                className="h-16 w-auto object-contain mx-auto"
                priority
                placeholder="blur"
                blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiBmaWxsPSIjRjNGNEY2Ii8+Cjwvc3ZnPgo="
              />
            </Link>
          </div>

          {children}

          <div className="text-center mt-6">
            <Link
              href="/"
              className="text-brand-secondary hover:text-brand-primary transition-colors"
            >
              ← 홈으로 돌아가기
            </Link>
          </div>
        </div>
      </div>

      <HomeFooter />
      <AuthLayoutClient />
    </div>
  );
});