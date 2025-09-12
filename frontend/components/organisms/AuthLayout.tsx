"use client";

import Link from "next/link";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { ToastContainer } from "@/components";
import { useToast } from "@/hooks/useToast";

interface AuthLayoutProps {
  children: React.ReactNode;
  title?: string;
  description?: string;
}

export function AuthLayout({ children, title, description }: AuthLayoutProps) {
  const { toasts, removeToast } = useToast();

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />
      
      <div className="flex items-center justify-center p-4 py-16">
        <div className="w-full max-w-md">
          {/* Logo */}
          <div className="text-center mb-8">
            <Link href="/" className="inline-block">
              <img
                src="/log.png"
                alt="비밀로그"
                className="h-16 object-contain mx-auto"
              />
            </Link>
          </div>

          {children}

          <div className="text-center mt-6">
            <Link
              href="/"
              className="text-gray-500 hover:text-gray-700 transition-colors"
            >
              ← 홈으로 돌아가기
            </Link>
          </div>
        </div>
      </div>

      <HomeFooter />
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </div>
  );
}