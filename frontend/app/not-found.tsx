"use client";

import Link from "next/link";
import { Button } from "@/components";
import { Heart, Home, Search, ArrowLeft } from "lucide-react";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <div className="flex items-center justify-center p-4 py-16">
        <div className="text-center max-w-md mx-auto">
          {/* 404 일러스트레이션 */}
          <div className="mb-8">
            <div className="relative">
              {/* 메인 아이콘 */}
              <div className="w-24 h-24 mx-auto mb-6 flex items-center justify-center">
                <img
                  src="/log.png"
                  alt="비밀로그"
                  className="h-24 object-contain"
                />
              </div>

              {/* 404 숫자 */}
              <div className="text-6xl md:text-7xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent mb-4">
                404
              </div>

              {/* 장식용 요소들 */}
              <div
                className="absolute -top-4 -left-4 w-8 h-8 bg-pink-200 rounded-full opacity-60 animate-bounce"
                style={{ animationDelay: "0s" }}
              ></div>
              <div
                className="absolute -top-2 -right-6 w-6 h-6 bg-purple-200 rounded-full opacity-60 animate-bounce"
                style={{ animationDelay: "0.5s" }}
              ></div>
              <div
                className="absolute -bottom-4 left-8 w-4 h-4 bg-indigo-200 rounded-full opacity-60 animate-bounce"
                style={{ animationDelay: "1s" }}
              ></div>
            </div>
          </div>

          {/* 텍스트 영역 */}
          <div className="mb-8">
            <h1 className="text-2xl md:text-3xl font-bold text-gray-800 mb-4">
              페이지를 찾을 수 없어요
            </h1>
            <p className="text-gray-600 leading-relaxed">
              요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요.
              <br />
              다른 페이지를 둘러보시거나 홈으로 돌아가보세요.
            </p>
          </div>

          {/* 액션 버튼들 */}
          <div className="space-y-3">
            <Button asChild size="lg" className="w-full">
              <Link href="/">
                <Home className="w-5 h-5 mr-2" />
                홈으로 돌아가기
              </Link>
            </Button>

            <div className="grid grid-cols-2 gap-3">
              <Button asChild variant="outline" size="lg">
                <Link href="/board">
                  <Search className="w-4 h-4 mr-2" />
                  게시판
                </Link>
              </Button>

              <Button asChild variant="outline" size="lg">
                <Link href="/visit">
                  <Heart className="w-4 h-4 mr-2" />
                  롤링페이퍼
                </Link>
              </Button>
            </div>

            <Button
              variant="ghost"
              size="lg"
              onClick={() => window.history.back()}
              className="w-full"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              이전 페이지로
            </Button>
          </div>

          {/* 하단 링크들 */}
          <div className="mt-12 pt-8 border-t border-gray-200">
            <p className="text-sm text-gray-500 mb-4">도움이 필요하신가요?</p>
            <div className="flex justify-center space-x-6 text-sm">
              <Link
                href="/suggest"
                className="text-purple-600 hover:text-purple-700 hover:underline transition-colors"
              >
                건의하기
              </Link>
              <Link
                href="/help"
                className="text-purple-600 hover:text-purple-700 hover:underline transition-colors"
              >
                도움말
              </Link>
            </div>
          </div>
        </div>
      </div>

      <HomeFooter />
    </div>
  );
}
