"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Heart,
  MessageCircle,
  Users,
  Sparkles,
  UserCheck,
  Share2,
} from "lucide-react";
import Link from "next/link";
import { AuthHeader } from "@/components/organisms/auth-header";
import { useAuth } from "@/hooks/useAuth";
import { useState } from "react";
import { KakaoFriendsModal } from "@/components/molecules/kakao-friends-modal";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";

export default function HomePage() {
  const { isAuthenticated } = useAuth();
  const [isFriendsModalOpen, setIsFriendsModalOpen] = useState(false);

  const handleOpenFriendsModal = () => {
    if (!isAuthenticated) return;
    setIsFriendsModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 md:py-24 text-center">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
            익명으로 마음을 전해보세요
          </h1>
          <p className="text-xl md:text-2xl text-gray-600 mb-8 leading-relaxed">
            비밀로그에서 소중한 사람에게 익명의 따뜻한 메시지를 남겨보세요
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            {!isAuthenticated && (
              <Button
                size="lg"
                asChild
                className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 px-8 py-3 text-lg"
              >
                <Link href="/login">내 롤링페이퍼 만들기</Link>
              </Button>
            )}
            {isAuthenticated && (
              <Button
                size="lg"
                onClick={handleOpenFriendsModal}
                className="bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 px-8 py-3 text-lg text-yellow-900 font-bold shadow-lg"
              >
                <UserCheck className="w-5 h-5 mr-2" />
                카카오 친구 확인하기
              </Button>
            )}
            <Button
              size="lg"
              variant="outline"
              asChild
              className="border-purple-200 text-purple-600 hover:bg-purple-50 px-8 py-3 text-lg"
            >
              <Link href="/visit">다른 롤링페이퍼 방문하기</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="container mx-auto px-4 py-16">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-gray-800">
          비밀로그의 특별한 기능들
        </h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <Heart className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                익명 메시지
              </h3>
              <p className="text-gray-600 text-sm">
                로그인 없이도 누구나 익명으로 따뜻한 메시지를 남길 수 있어요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-orange-500 to-yellow-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                카카오 연동
              </h3>
              <p className="text-gray-600 text-sm">
                카카오톡으로 간편하게 로그인하고 친구들에게 공유해보세요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <Sparkles className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                다양한 디자인
              </h3>
              <p className="text-gray-600 text-sm">
                예쁜 디자인으로 메시지를 꾸며서 더욱 특별하게 만들어보세요
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardContent className="p-6 text-center">
              <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-teal-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <MessageCircle className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-gray-800">
                커뮤니티
              </h3>
              <p className="text-gray-600 text-sm">
                다른 사용자들과 소통하고 인기글을 확인해보세요
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container mx-auto px-4 py-16">
        <Card className="border-0 shadow-2xl bg-gradient-to-r from-pink-500 via-purple-600 to-indigo-600 text-white">
          <CardContent className="p-8 md:p-12 text-center">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">
              지금 시작해보세요!
            </h2>
            <p className="text-xl mb-8 opacity-90">
              소중한 사람들과 특별한 추억을 만들어보세요
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Button
                size="lg"
                className="bg-white text-purple-600 hover:bg-gray-100 px-8 py-3 text-lg font-semibold"
                asChild
              >
                <Link href={isAuthenticated ? "/rolling-paper" : "/signup"}>
                  {isAuthenticated ? "내 롤링페이퍼 보기" : "무료로 시작하기"}
                </Link>
              </Button>

              <KakaoShareButton
                type="service"
                variant="outline"
                size="lg"
                className="px-8 py-3 text-lg font-semibold min-h-[44px]"
              />
            </div>
          </CardContent>
        </Card>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="container mx-auto px-4">
          <div className="grid md:grid-cols-5 gap-8">
            <div>
              <div className="flex items-center mb-4">
                <img
                  src="/log.png"
                  alt="비밀로그"
                  className="h-10 object-contain"
                />
              </div>
              <p className="text-gray-400">
                익명으로 마음을 전하는 특별한 공간
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-4">서비스</h3>
              <ul className="space-y-2 text-gray-400">
                <li>
                  <Link
                    href="/board"
                    className="hover:text-white transition-colors"
                  >
                    게시판
                  </Link>
                </li>
                <li>
                  <Link
                    href="/visit"
                    className="hover:text-white transition-colors"
                  >
                    롤링페이퍼 방문
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">고객지원</h3>
              <ul className="space-y-2 text-gray-400">
                <li>
                  <Link
                    href="/suggest"
                    className="hover:text-white transition-colors"
                  >
                    건의하기
                  </Link>
                </li>
                <li>
                  <Link
                    href="/help"
                    className="hover:text-white transition-colors"
                  >
                    도움말
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">정책</h3>
              <ul className="space-y-2 text-gray-400">
                <li>
                  <Link
                    href="/privacy"
                    className="hover:text-white transition-colors"
                  >
                    개인정보처리방침
                  </Link>
                </li>
                <li>
                  <Link
                    href="/terms"
                    className="hover:text-white transition-colors"
                  >
                    이용약관
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">운영</h3>
              <ul className="space-y-2 text-gray-400">
                <li>
                  <a
                    href="https://cyclic-icebreaker-daa.notion.site/1d4a9f47800c80a1b12fc2aae7befd0e"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-white transition-colors"
                  >
                    개발자 노션
                  </a>
                </li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 mt-8 pt-8 text-center text-gray-400">
            <p>&copy; 2025 비밀로그. All rights reserved.</p>
          </div>
        </div>
      </footer>

      {/* 카카오 친구 모달 */}
      <KakaoFriendsModal
        isOpen={isFriendsModalOpen}
        onClose={() => setIsFriendsModalOpen(false)}
      />
    </div>
  );
}
