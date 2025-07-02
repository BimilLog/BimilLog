"use client";

import { Button } from "@/components/atoms/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Download,
  Smartphone,
  Monitor,
  Heart,
  MessageCircle,
  Users,
  Sparkles,
  ArrowLeft,
} from "lucide-react";
import Link from "next/link";
import { PWAInstallButton } from "@/components/molecules/pwa-install-button";
import { useBrowserGuide } from "@/hooks/useBrowserGuide";
import { useState, useEffect } from "react";

export default function InstallPage() {
  const [isClient, setIsClient] = useState(false);
  const [browserInfo, setBrowserInfo] = useState({
    name: "브라우저",
    isInApp: false,
  });
  const [isIOS, setIsIOS] = useState(false);
  const { getBrowserInfo } = useBrowserGuide();

  useEffect(() => {
    setIsClient(true);
    setBrowserInfo(getBrowserInfo());
    if (typeof navigator !== "undefined") {
      setIsIOS(
        /iPad|iPhone|iPod/.test(navigator.userAgent) &&
          !(window as any).MSStream
      );
    }
  }, [getBrowserInfo]);

  // 클라이언트에서만 렌더링되도록 보장
  if (!isClient) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <div className="container mx-auto px-4 py-16 text-center">
          <div className="text-6xl mb-6">📱</div>
          <h1 className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
            비밀로그를 앱으로 설치하세요
          </h1>
          <div className="animate-pulse">로딩 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="bg-white/80 backdrop-blur-sm border-b border-purple-100 sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4 flex items-center gap-4">
          <Button variant="ghost" size="sm" asChild>
            <Link href="/" className="flex items-center gap-2">
              <ArrowLeft className="w-4 h-4" />
              뒤로가기
            </Link>
          </Button>
          <div className="flex items-center gap-2">
            <img src="/log.png" alt="비밀로그" className="h-8 object-contain" />
            <span className="font-bold text-lg">비밀로그 설치</span>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 md:py-24 text-center">
        <div className="max-w-4xl mx-auto">
          <div className="text-6xl mb-6">📱</div>
          <h1 className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
            비밀로그를 앱으로 설치하세요
          </h1>
          <p className="text-xl md:text-2xl text-gray-600 mb-8 leading-relaxed">
            더 빠르고 편리한 앱 경험을 즐겨보세요
          </p>

          {/* 메인 설치 버튼 */}
          <div className="flex flex-col gap-4 items-center mb-12">
            <PWAInstallButton
              size="lg"
              className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 px-12 py-4 text-xl font-bold shadow-lg transform transition-transform hover:scale-105"
            />
            <p className="text-sm text-gray-500">
              현재 브라우저: {browserInfo.name}
            </p>
          </div>

          {/* 설치 방법 안내 */}
          {isIOS ? (
            <Card className="bg-blue-50 border-blue-200 max-w-2xl mx-auto">
              <CardContent className="p-6">
                <h3 className="font-bold text-blue-800 mb-4 flex items-center justify-center gap-2">
                  <Smartphone className="w-5 h-5" />
                  iPhone/iPad 설치 방법
                </h3>
                <ol className="text-left text-blue-700 space-y-2">
                  <li className="flex gap-3">
                    <span className="font-bold">1.</span>
                    <span>Safari 브라우저에서 이 페이지를 여세요</span>
                  </li>
                  <li className="flex gap-3">
                    <span className="font-bold">2.</span>
                    <span>
                      하단 메뉴의 <span className="font-bold">[공유]</span>{" "}
                      버튼을 누르세요
                    </span>
                  </li>
                  <li className="flex gap-3">
                    <span className="font-bold">3.</span>
                    <span>
                      <span className="font-bold">[홈 화면에 추가]</span>를
                      선택하면 설치 완료!
                    </span>
                  </li>
                </ol>
              </CardContent>
            </Card>
          ) : (
            <Card className="bg-green-50 border-green-200 max-w-2xl mx-auto">
              <CardContent className="p-6">
                <h3 className="font-bold text-green-800 mb-4 flex items-center justify-center gap-2">
                  <Monitor className="w-5 h-5" />
                  Android/PC 설치 방법
                </h3>
                <ol className="text-left text-green-700 space-y-2">
                  <li className="flex gap-3">
                    <span className="font-bold">1.</span>
                    <span>Chrome 브라우저에서 이 페이지를 여세요</span>
                  </li>
                  <li className="flex gap-3">
                    <span className="font-bold">2.</span>
                    <span>
                      위의 <span className="font-bold">"앱 설치"</span> 버튼을
                      클릭하세요
                    </span>
                  </li>
                  <li className="flex gap-3">
                    <span className="font-bold">3.</span>
                    <span>
                      설치 확인 창에서 <span className="font-bold">"설치"</span>
                      를 누르면 완료!
                    </span>
                  </li>
                </ol>
              </CardContent>
            </Card>
          )}
        </div>
      </section>

      {/* 앱 설치의 장점 */}
      <section className="container mx-auto px-4 py-16">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-gray-800">
          앱으로 설치하면 더 좋은 점
        </h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <Download className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                빠른 접속
              </h3>
              <p className="text-gray-600 text-sm">
                홈 화면에서 바로 실행할 수 있어 더욱 빠르게 접속 가능해요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <MessageCircle className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                알림 받기
              </h3>
              <p className="text-gray-600 text-sm">
                새로운 메시지나 댓글 알림을 바로 받아볼 수 있어요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-teal-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <Sparkles className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                오프라인 지원
              </h3>
              <p className="text-gray-600 text-sm">
                이전에 본 내용은 계속 확인할 수 있어요
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* 미리보기 스크린샷 */}
      <section className="container mx-auto px-4 py-16">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-gray-800">
          앱 미리보기
        </h2>
        <div className="max-w-4xl mx-auto">
          <Card className="overflow-hidden shadow-2xl">
            <div className="bg-gradient-to-r from-pink-500 to-purple-600 p-8 text-center">
              <img
                src="/log.png"
                alt="비밀로그 앱 미리보기"
                className="h-32 object-contain mx-auto filter brightness-0 invert"
              />
              <h3 className="text-2xl font-bold text-white mt-4">비밀로그</h3>
              <p className="text-pink-100">
                익명으로 마음을 전하는 특별한 공간
              </p>
            </div>
          </Card>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center mb-4">
            <img
              src="/log.png"
              alt="비밀로그"
              className="h-10 object-contain filter brightness-0 invert"
            />
          </div>
          <p className="text-gray-400 mb-4">
            익명으로 마음을 전하는 특별한 공간
          </p>
          <div className="flex justify-center gap-6 text-sm">
            <Link
              href="/privacy"
              className="text-gray-400 hover:text-white transition-colors"
            >
              개인정보처리방침
            </Link>
            <Link
              href="/terms"
              className="text-gray-400 hover:text-white transition-colors"
            >
              이용약관
            </Link>
          </div>
          <p className="text-gray-500 text-sm mt-4">
            &copy; 2025 비밀로그. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
