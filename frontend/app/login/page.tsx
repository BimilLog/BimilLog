"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MessageSquare } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

export default function LoginPage() {
  const { login, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <img
            src="/log.png"
            alt="비밀로그"
            className="h-12 object-contain mx-auto mb-4 animate-pulse"
          />
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    );
  }

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

          <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
            <CardHeader className="text-center pb-2">
              <CardTitle className="text-2xl font-bold text-gray-800">
                로그인
              </CardTitle>
              <CardDescription className="text-gray-600">
                나만의 롤링페이퍼를 만들어 보세요
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6 pt-6">
              <Button
                className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-gray-800 font-semibold text-base"
                onClick={() => login()}
              >
                <svg
                  className="w-5 h-5 mr-2"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z" />
                </svg>
                카카오로 시작하기
              </Button>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-start space-x-2">
                  <MessageSquare className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
                  <div className="text-sm text-blue-800">
                    <p className="font-medium mb-1">로그인 없이도 이용 가능!</p>
                    <p>
                      로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수
                      있어요.
                    </p>
                    <p>게시판 이용도 할 수 있어요.</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

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
    </div>
  );
}
