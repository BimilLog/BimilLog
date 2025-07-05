"use client";

import { Button } from "@/components/atoms/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Smartphone,
  Monitor,
  MessageCircle,
  ArrowLeft,
  Zap,
  Shield,
  Wifi,
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
      <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50">
        <div className="container mx-auto px-4 py-16 text-center">
          <div className="text-6xl mb-6 animate-pulse">📱</div>
          <h1 className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-cyan-600 via-blue-600 to-teal-600 bg-clip-text text-transparent">
            비밀로그를 앱으로 설치하세요
          </h1>
          <div className="animate-pulse text-cyan-600">로딩 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50">
      {/* Header */}
      <header className="bg-white/90 backdrop-blur-lg border-b border-cyan-200 sticky top-0 z-50 shadow-sm">
        <div className="container mx-auto px-4 py-4 flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            asChild
            className="hover:bg-cyan-100 text-cyan-700 transition-colors"
          >
            <Link href="/" className="flex items-center gap-2">
              <ArrowLeft className="w-4 h-4" />
              뒤로가기
            </Link>
          </Button>
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gradient-to-r from-cyan-500 to-teal-500 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">📱</span>
            </div>
            <span className="font-bold text-lg text-gray-800">
              비밀로그 설치
            </span>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 md:py-24 text-center">
        <div className="max-w-4xl mx-auto">
          <div className="text-8xl mb-8 animate-bounce">📱</div>
          <h1 className="text-5xl md:text-7xl font-bold mb-8 bg-gradient-to-r from-cyan-600 via-blue-600 to-teal-600 bg-clip-text text-transparent leading-tight">
            비밀로그를
            <br className="md:hidden" />
            <span className="block">앱으로 설치하세요</span>
          </h1>
          <p className="text-xl md:text-2xl text-gray-600 mb-12 leading-relaxed max-w-2xl mx-auto">
            더 빠르고 편리한 앱 경험으로
            <br />
            언제 어디서나 마음을 전해보세요 💌
          </p>

          {/* 메인 설치 버튼 */}
          <div className="flex flex-col gap-6 items-center mb-16">
            <PWAInstallButton
              size="lg"
              className="bg-gradient-to-r from-cyan-500 to-teal-600 hover:from-cyan-600 hover:to-teal-700 px-16 py-5 text-xl font-bold shadow-xl transform transition-all duration-300 hover:scale-105 hover:shadow-2xl rounded-2xl"
            />
            <div className="flex items-center gap-2 text-sm text-gray-500 bg-white/60 backdrop-blur-sm px-4 py-2 rounded-full">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
              현재 브라우저: {browserInfo.name}
            </div>
          </div>

          {/* 설치 방법 안내 */}
          {isIOS ? (
            <Card className="bg-gradient-to-r from-blue-50 to-cyan-50 border-2 border-blue-200 max-w-2xl mx-auto shadow-xl">
              <CardContent className="p-8">
                <h3 className="font-bold text-blue-800 mb-6 flex items-center justify-center gap-3 text-xl">
                  <Smartphone className="w-6 h-6" />
                  iPhone/iPad 설치 방법
                </h3>
                <ol className="text-left text-blue-700 space-y-4">
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-blue-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      1
                    </span>
                    <span className="text-base">
                      Safari 브라우저에서 이 페이지를 여세요
                    </span>
                  </li>
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-blue-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      2
                    </span>
                    <span className="text-base">
                      하단 메뉴의{" "}
                      <span className="font-bold bg-blue-100 px-2 py-1 rounded">
                        [공유]
                      </span>{" "}
                      버튼을 누르세요
                    </span>
                  </li>
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-blue-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      3
                    </span>
                    <span className="text-base">
                      <span className="font-bold bg-blue-100 px-2 py-1 rounded">
                        [홈 화면에 추가]
                      </span>
                      를 선택하면 설치 완료! 🎉
                    </span>
                  </li>
                </ol>
              </CardContent>
            </Card>
          ) : (
            <Card className="bg-gradient-to-r from-green-50 to-emerald-50 border-2 border-green-200 max-w-2xl mx-auto shadow-xl">
              <CardContent className="p-8">
                <h3 className="font-bold text-green-800 mb-6 flex items-center justify-center gap-3 text-xl">
                  <Monitor className="w-6 h-6" />
                  Android/PC 설치 방법
                </h3>
                <ol className="text-left text-green-700 space-y-4">
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-green-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      1
                    </span>
                    <span className="text-base">
                      Chrome 브라우저에서 이 페이지를 여세요
                    </span>
                  </li>
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-green-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      2
                    </span>
                    <span className="text-base">
                      위의{" "}
                      <span className="font-bold bg-green-100 px-2 py-1 rounded">
                        "앱 설치"
                      </span>{" "}
                      버튼을 클릭하세요
                    </span>
                  </li>
                  <li className="flex gap-4 items-start">
                    <span className="font-bold text-lg bg-green-100 w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0">
                      3
                    </span>
                    <span className="text-base">
                      설치 확인 창에서{" "}
                      <span className="font-bold bg-green-100 px-2 py-1 rounded">
                        "설치"
                      </span>
                      를 누르면 완료! 🎉
                    </span>
                  </li>
                </ol>
              </CardContent>
            </Card>
          )}
        </div>
      </section>

      {/* 앱 설치의 장점 */}
      <section className="container mx-auto px-4 py-20 bg-white/30 backdrop-blur-sm">
        <h2 className="text-4xl md:text-5xl font-bold text-center mb-4 text-gray-800">
          앱으로 설치하면 더 좋은 점
        </h2>
        <p className="text-center text-gray-600 mb-16 text-lg max-w-2xl mx-auto">
          브라우저보다 훨씬 빠르고 편리한 앱 경험을 제공합니다
        </p>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
          <Card className="border-0 shadow-xl hover:shadow-2xl transition-all duration-300 bg-white/90 backdrop-blur-sm hover:scale-105 group">
            <CardContent className="p-8 text-center">
              <div className="w-16 h-16 bg-gradient-to-r from-cyan-500 to-blue-500 rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <Zap className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold mb-4 text-gray-800">
                빠른 접속
              </h3>
              <p className="text-gray-600 leading-relaxed">
                홈 화면에서 바로 실행할 수 있어 더욱 빠르게 접속 가능해요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-xl hover:shadow-2xl transition-all duration-300 bg-white/90 backdrop-blur-sm hover:scale-105 group">
            <CardContent className="p-8 text-center">
              <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <MessageCircle className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold mb-4 text-gray-800">
                알림 받기
              </h3>
              <p className="text-gray-600 leading-relaxed">
                새로운 메시지나 댓글 알림을 바로 받아볼 수 있어요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-xl hover:shadow-2xl transition-all duration-300 bg-white/90 backdrop-blur-sm hover:scale-105 group">
            <CardContent className="p-8 text-center">
              <div className="w-16 h-16 bg-gradient-to-r from-green-500 to-teal-500 rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <Wifi className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold mb-4 text-gray-800">
                오프라인 지원
              </h3>
              <p className="text-gray-600 leading-relaxed">
                인터넷 연결이 없어도 이전에 본 내용을 계속 확인할 수 있어요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-xl hover:shadow-2xl transition-all duration-300 bg-white/90 backdrop-blur-sm hover:scale-105 group">
            <CardContent className="p-8 text-center">
              <div className="w-16 h-16 bg-gradient-to-r from-orange-500 to-red-500 rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <Shield className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold mb-4 text-gray-800">
                보안 강화
              </h3>
              <p className="text-gray-600 leading-relaxed">
                전용 앱으로 더 안전하고 개인적인 공간을 제공해요
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* 미리보기 스크린샷 */}
      <section className="container mx-auto px-4 py-20">
        <h2 className="text-4xl md:text-5xl font-bold text-center mb-4 text-gray-800">
          앱 미리보기
        </h2>
        <p className="text-center text-gray-600 mb-16 text-lg max-w-2xl mx-auto">
          실제 앱처럼 동작하는 비밀로그의 모습을 확인해보세요
        </p>
        <div className="max-w-6xl mx-auto">
          {/* 스크린샷 갤러리 */}
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 mb-12">
            {/* 메인 페이지 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_mainpage_mobile.png"
                  alt="비밀로그 메인 페이지"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">메인 페이지</h3>
                  <p className="text-sm text-gray-200">
                    깔끔하고 직관적인 홈 화면
                  </p>
                </div>
              </div>
            </Card>

            {/* 게시판 페이지 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_board_mobile.png"
                  alt="비밀로그 게시판"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">커뮤니티 게시판</h3>
                  <p className="text-sm text-gray-200">
                    익명으로 소통하는 공간
                  </p>
                </div>
              </div>
            </Card>

            {/* 마이페이지 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_mypage_mobile.png"
                  alt="비밀로그 마이페이지"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">마이페이지</h3>
                  <p className="text-sm text-gray-200">내 활동을 한눈에 확인</p>
                </div>
              </div>
            </Card>

            {/* 롤링페이퍼 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_mypaper_mobile.png"
                  alt="비밀로그 롤링페이퍼"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">롤링페이퍼</h3>
                  <p className="text-sm text-gray-200">
                    친구들의 따뜻한 메시지
                  </p>
                </div>
              </div>
            </Card>

            {/* 메시지 작성 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_messageform.png"
                  alt="비밀로그 메시지 작성"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">메시지 작성</h3>
                  <p className="text-sm text-gray-200">
                    다양한 디자인으로 메시지 전달
                  </p>
                </div>
              </div>
            </Card>

            {/* 방문 페이지 */}
            <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
              <div className="aspect-[9/16] relative">
                <img
                  src="/bimillog_visit_mobile.png"
                  alt="비밀로그 방문 페이지"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
                <div className="absolute bottom-4 left-4 right-4 text-white">
                  <h3 className="font-bold text-lg mb-1">방문 페이지</h3>
                  <p className="text-sm text-gray-200">
                    친구의 롤링페이퍼 방문
                  </p>
                </div>
              </div>
            </Card>
          </div>

          {/* 특별한 기능 강조 */}
          <Card className="bg-gradient-to-r from-cyan-500 via-blue-500 to-teal-500 p-8 text-center relative overflow-hidden">
            <div className="absolute inset-0 bg-white/10 backdrop-blur-sm"></div>
            <div className="relative z-10">
              <div className="w-20 h-20 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-6">
                <span className="text-4xl">✨</span>
              </div>
              <h3 className="text-2xl font-bold text-white mb-4">
                모든 기능을 앱에서 더 편리하게!
              </h3>
              <p className="text-cyan-100 text-lg max-w-2xl mx-auto">
                푸시 알림, 오프라인 지원, 빠른 실행 등 네이티브 앱과 같은 경험을
                제공합니다
              </p>
            </div>
            <div className="absolute top-4 right-4 w-6 h-6 bg-white/30 rounded-full"></div>
            <div className="absolute bottom-8 left-8 w-4 h-4 bg-white/20 rounded-full"></div>
          </Card>
        </div>
      </section>

      {/* Call to Action */}
      <section className="container mx-auto px-4 py-20 text-center">
        <div className="max-w-2xl mx-auto">
          <h2 className="text-3xl md:text-4xl font-bold mb-6 text-gray-800">
            지금 바로 설치해보세요! 🚀
          </h2>
          <p className="text-xl text-gray-600 mb-8">
            더 나은 비밀로그 경험이 기다리고 있습니다
          </p>
          <PWAInstallButton
            size="lg"
            className="bg-gradient-to-r from-cyan-500 to-teal-600 hover:from-cyan-600 hover:to-teal-700 px-12 py-4 text-lg font-bold shadow-xl transform transition-all duration-300 hover:scale-105"
          />
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-16">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center mb-6">
            <h2 className="text-3xl font-bold text-white">비밀로그</h2>
          </div>
          <p className="text-gray-400 mb-8 text-lg">
            익명으로 마음을 전하는 특별한 공간
          </p>
          <div className="flex justify-center gap-8 text-base mb-8">
            <Link
              href="/privacy"
              className="text-gray-400 hover:text-white transition-colors hover:underline"
            >
              개인정보처리방침
            </Link>
            <Link
              href="/terms"
              className="text-gray-400 hover:text-white transition-colors hover:underline"
            >
              이용약관
            </Link>
          </div>
          <div className="border-t border-gray-800 pt-8">
            <p className="text-gray-500 text-sm">
              &copy; 2025 비밀로그. All rights reserved. v1.0.12
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
