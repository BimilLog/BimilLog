"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Heart, MessageCircle, Users, Sparkles } from "lucide-react";
import Image from "next/image";

export const HomeFeatures: React.FC = () => {
  const features = [
    {
      icon: Heart,
      title: "익명 메시지",
      description:
        "로그인 없이도 누구나 익명으로 따뜻한 메시지를 남길 수 있어요",
      gradient: "from-pink-500 to-red-500",
    },
    {
      icon: Users,
      title: "카카오 연동",
      description: "카카오톡으로 간편하게 로그인하고 친구들에게 공유해보세요",
      gradient: "from-orange-500 to-yellow-500",
    },
    {
      icon: Sparkles,
      title: "다양한 디자인",
      description: "예쁜 디자인으로 메시지를 꾸며서 더욱 특별하게 만들어보세요",
      gradient: "from-purple-500 to-indigo-500",
    },
    {
      icon: MessageCircle,
      title: "커뮤니티",
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요",
      gradient: "from-green-500 to-teal-500",
    },
  ];

  return (
    <>
      <section className="container mx-auto px-4 py-16">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-gray-800">
          비밀로그의 특별한 기능들
        </h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, index) => {
            const IconComponent = feature.icon;
            return (
              <Card
                key={index}
                className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm"
              >
                <CardContent className="p-6 text-center">
                  <div
                    className={`w-12 h-12 bg-gradient-to-r ${feature.gradient} rounded-full flex items-center justify-center mx-auto mb-4`}
                  >
                    <IconComponent className="w-6 h-6 text-white" />
                  </div>
                  <h3 className="text-lg font-semibold mb-2 text-gray-800">
                    {feature.title}
                  </h3>
                  <p className="text-gray-600 text-sm">{feature.description}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      {/* 앱 미리보기 섹션 */}
      <section className="container mx-auto px-4 py-16 bg-white/30 backdrop-blur-sm">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-4 text-gray-800">
          실제 화면 미리보기
        </h2>
        <p className="text-center text-gray-600 mb-12 text-lg max-w-2xl mx-auto">
          비밀로그의 실제 모습을 확인해보세요
        </p>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-6xl mx-auto">
          {/* 메인 페이지 */}
          <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
            <div className="aspect-[9/16] relative">
              <Image
                src="/bimillog_mainpage_mobile.png"
                alt="비밀로그 메인 페이지"
                fill
                className="object-cover"
                sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
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

          {/* 롤링페이퍼 */}
          <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
            <div className="aspect-[9/16] relative">
              <Image
                src="/bimillog_mypaper_mobile.png"
                alt="비밀로그 롤링페이퍼"
                fill
                className="object-cover"
                sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
              <div className="absolute bottom-4 left-4 right-4 text-white">
                <h3 className="font-bold text-lg mb-1">롤링페이퍼</h3>
                <p className="text-sm text-gray-200">친구들의 따뜻한 메시지</p>
              </div>
            </div>
          </Card>

          {/* 게시판 */}
          <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
            <div className="aspect-[9/16] relative">
              <Image
                src="/bimillog_board_mobile.png"
                alt="비밀로그 게시판"
                fill
                className="object-cover"
                sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent"></div>
              <div className="absolute bottom-4 left-4 right-4 text-white">
                <h3 className="font-bold text-lg mb-1">커뮤니티</h3>
                <p className="text-sm text-gray-200">익명으로 소통하는 공간</p>
              </div>
            </div>
          </Card>

          {/* 메시지 작성 */}
          <Card className="overflow-hidden shadow-xl hover:shadow-2xl transition-all duration-300 hover:scale-105">
            <div className="aspect-[9/16] relative">
              <Image
                src="/bimillog_messageform.png"
                alt="비밀로그 메시지 작성"
                fill
                className="object-cover"
                sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
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
        </div>
      </section>
    </>
  );
};
