"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import {
  rollingPaperApi,
  type RollingPaperMessage,
  getDecoInfo,
} from "@/lib/api";
import { useRouter } from "next/navigation";
import {
  MessageSquare,
  Plus,
  ArrowLeft,
  Share2,
  Heart,
  Info,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import Link from "next/link";
import { AuthHeader } from "@/components/organisms/auth-header";

// 분리된 컴포넌트들 import
import { MessageForm } from "./components/MessageForm";
import { MessageView } from "./components/MessageView";

export default function RollingPaperPage() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [selectedCell, setSelectedCell] = useState<number | null>(null);
  const [messages, setMessages] = useState<{
    [key: number]: RollingPaperMessage;
  }>({});
  const [isLoadingMessages, setIsLoadingMessages] = useState(true);
  const [isMobile, setIsMobile] = useState(false);

  // 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  // 모바일 여부 감지
  useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkIsMobile();
    window.addEventListener("resize", checkIsMobile);

    return () => window.removeEventListener("resize", checkIsMobile);
  }, []);

  // 내 롤링페이퍼 메시지 조회
  useEffect(() => {
    const fetchMessages = async () => {
      if (!isAuthenticated || !user) return;

      try {
        const response = await rollingPaperApi.getMyRollingPaper();
        if (response.success && response.data) {
          const messageMap: { [key: number]: RollingPaperMessage } = {};
          response.data.forEach((message) => {
            const position = message.height * 6 + message.width; // 6칸으로 변경
            messageMap[position] = message;
          });
          setMessages(messageMap);
        }
      } catch (error) {
        console.error("Failed to fetch messages:", error);
      } finally {
        setIsLoadingMessages(false);
      }
    };

    fetchMessages();
  }, [isAuthenticated, user]);

  const handleShare = async () => {
    if (!user) return;
    const url = `${window.location.origin}/rolling-paper/${encodeURIComponent(
      user.userName
    )}`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: `${user.userName}님의 롤링페이퍼`,
          text: "익명으로 따뜻한 메시지를 남겨보세요!",
          url: url,
        });
      } catch (error) {
        console.log("Share cancelled");
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
        alert("링크가 클립보드에 복사되었습니다!");
      } catch (error) {
        console.error("Failed to copy to clipboard:", error);
      }
    }
  };

  if (isLoading || isLoadingMessages) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50 flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600 font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return null;
  }

  const handleMessageSubmit = (index: number, data: any) => {
    setMessages((prev) => ({
      ...prev,
      [index]: {
        id: Date.now(),
        userId: user?.userId || 0,
        decoType: data.decoType,
        anonymity: data.anonymousNickname,
        content: data.content,
        width: index % 6, // 6칸으로 변경
        height: Math.floor(index / 6),
        createdAt: new Date().toISOString(),
        isDeleted: false,
      },
    }));
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />

      {/* Page Header - 모바일 최적화 */}
      <header className="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-3 md:py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 md:space-x-4 flex-1 min-w-0">
              <Link href="/">
                <Button variant="ghost" size="sm" className="h-8 md:h-10">
                  <ArrowLeft className="w-4 h-4 mr-1 md:mr-2" />
                  <span className="hidden sm:inline">홈으로</span>
                  <span className="sm:hidden">홈</span>
                </Button>
              </Link>
              <div className="flex items-center space-x-2 flex-1 min-w-0">
                <div className="w-6 h-6 md:w-8 md:h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                  <MessageSquare className="w-3 h-3 md:w-5 md:h-5 text-white" />
                </div>
                <div className="min-w-0 flex-1">
                  <h1 className="font-bold text-gray-800 text-sm md:text-base truncate">
                    {user.userName}님의 롤링페이퍼
                  </h1>
                  <p className="text-xs text-gray-500">
                    총 {Object.keys(messages).length}개
                  </p>
                </div>
              </div>
            </div>
            <div className="flex items-center space-x-1 md:space-x-2">
              <KakaoShareButton
                type="rollingPaper"
                userName={user.userName}
                messageCount={Object.keys(messages).length}
                variant="outline"
                size="sm"
                className="h-8 md:h-10 px-2 md:px-3"
              />
              <Button
                variant="outline"
                size="sm"
                className="bg-white h-8 md:h-10 px-2 md:px-3"
                onClick={handleShare}
              >
                <Share2 className="w-4 h-4 md:mr-1" />
                <span className="hidden md:inline">공유</span>
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="bg-white h-8 md:h-10 px-2 md:px-3"
              >
                <Heart className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-2 md:px-4 py-4 md:py-8">
        {/* 자기 자신의 롤링페이퍼 안내 */}
        <div className="mb-4 md:mb-6">
          <Card className="bg-cyan-50/80 backdrop-blur-sm border-2 border-cyan-200 rounded-2xl">
            <CardContent className="p-4">
              <div className="flex items-start space-x-3">
                <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-cyan-800 font-semibold text-sm md:text-base">
                    내 롤링페이퍼 보기 모드 🌊
                  </p>
                  <p className="text-cyan-700 text-xs md:text-sm mt-1">
                    이곳은 나에게 온 메시지들을 볼 수 있는 공간이에요.
                    <span className="block md:inline">
                      {" "}
                      친구들에게 공유하여 메시지를 받아보세요! 💌
                    </span>
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Rolling Paper Grid - 모바일 최적화 */}
        <div className="relative max-w-5xl mx-auto mb-6 md:mb-8">
          {/* 종이 배경 - 모바일 조정 */}
          <div
            className="relative bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 rounded-2xl md:rounded-3xl shadow-xl md:shadow-2xl border-2 md:border-4 border-cyan-200"
            style={{
              backgroundImage: `
                radial-gradient(circle at 15px 15px, rgba(91,192,222,0.3) 1px, transparent 1px),
                radial-gradient(circle at 60px 60px, rgba(135,206,235,0.2) 1px, transparent 1px),
                linear-gradient(45deg, rgba(255,255,255,0.1) 25%, transparent 25%),
                linear-gradient(-45deg, rgba(255,255,255,0.1) 25%, transparent 25%)
              `,
              backgroundSize: "30px 30px, 120px 120px, 15px 15px, 15px 15px",
            }}
          >
            {/* 바인더 구멍들 - 모바일 조정 */}
            <div className="absolute left-3 md:left-6 top-0 bottom-0 flex flex-col justify-evenly">
              {Array.from({ length: 8 }, (_, i) => (
                <div
                  key={i}
                  className="w-4 h-4 md:w-6 md:h-6 bg-white rounded-full shadow-inner border border-cyan-300 md:border-2"
                  style={{
                    boxShadow:
                      "inset 0 1px 2px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.1)",
                  }}
                />
              ))}
            </div>

            {/* 제목 영역 - 모바일 조정 */}
            <div className="pt-6 md:pt-8 pb-4 md:pb-6 px-12 md:px-20 text-center">
              <div className="relative">
                {/* 현대적이면서 귀여운 제목 */}
                <h1 className="text-lg md:text-3xl font-bold text-cyan-800 mb-2 transform -rotate-1">
                  🌊 {user.userName}님의 롤링페이퍼 🌊
                </h1>

                {/* 시원한 데코레이션 - 모바일 조정 */}
                <div className="absolute -top-1 md:-top-2 -left-2 md:-left-4 text-lg md:text-2xl animate-bounce">
                  ❄️
                </div>
                <div className="absolute -top-1 -right-3 md:-right-6 text-sm md:text-xl animate-pulse">
                  ✨
                </div>
                <div className="absolute -bottom-1 md:-bottom-2 left-4 md:left-8 text-sm md:text-lg animate-bounce delay-300">
                  🧊
                </div>
                <div className="absolute -bottom-1 right-6 md:right-12 text-sm md:text-xl animate-pulse delay-500">
                  💎
                </div>

                <p className="text-cyan-600 text-xs md:text-sm mt-2 transform rotate-1 font-medium">
                  총 {Object.keys(messages).length}개의 시원한 메시지 💌
                </p>
              </div>
            </div>

            {/* 메시지 그리드 - 모바일 최적화 */}
            <div className="px-12 md:px-20 pb-8 md:pb-12">
              {/* 모바일: 4칸, 태블릿+: 6칸 */}
              <div className="grid grid-cols-4 md:grid-cols-6 gap-2 md:gap-3 bg-white/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-cyan-300">
                {Array.from({ length: isMobile ? 40 : 84 }, (_, i) => {
                  // 모바일: 4x10 = 40칸, 데스크톱: 6x14 = 84칸
                  const hasMessage = messages[i];
                  const decoInfo = hasMessage
                    ? getDecoInfo(hasMessage.decoType)
                    : null;

                  return (
                    <Dialog key={i}>
                      <DialogTrigger asChild>
                        <div
                          className={`
                            aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 
                            ${
                              hasMessage
                                ? `bg-gradient-to-br ${decoInfo?.color} border-white shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
                                : "border-dashed border-cyan-300 cursor-not-allowed opacity-50"
                            }
                          `}
                          style={{
                            boxShadow: hasMessage
                              ? "0 2px 8px rgba(91,192,222,0.3), inset 0 1px 0 rgba(255,255,255,0.5)"
                              : "0 1px 4px rgba(91,192,222,0.1)",
                          }}
                        >
                          {hasMessage ? (
                            <div className="relative">
                              <span className="text-lg md:text-2xl animate-bounce">
                                {decoInfo?.emoji}
                              </span>
                              {/* 반짝이는 효과 */}
                              <div className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-1.5 h-1.5 md:w-2 md:h-2 bg-yellow-300 rounded-full animate-ping"></div>
                            </div>
                          ) : (
                            <div className="text-cyan-300 text-xs md:text-sm text-center leading-tight">
                              읽기
                              <br className="md:hidden" />
                              전용
                            </div>
                          )}
                        </div>
                      </DialogTrigger>
                      {hasMessage && (
                        <DialogContent className="max-w-sm md:max-w-md mx-4 bg-gradient-to-br from-cyan-50 to-blue-50 border-2 md:border-4 border-cyan-200 rounded-2xl md:rounded-3xl">
                          <DialogHeader>
                            <DialogTitle className="text-center text-cyan-800 font-bold text-sm md:text-base">
                              💌 메시지 보기
                            </DialogTitle>
                          </DialogHeader>
                          <MessageView message={hasMessage} isOwner={true} />
                        </DialogContent>
                      )}
                    </Dialog>
                  );
                })}
              </div>
            </div>

            {/* 시원한 스티커들 - 모바일 조정 */}
            <div className="absolute top-8 md:top-16 right-4 md:right-8 text-xl md:text-3xl animate-spin-slow">
              🌟
            </div>
            <div className="absolute top-16 md:top-32 left-8 md:left-12 text-lg md:text-2xl animate-bounce">
              🐋
            </div>
            <div className="absolute bottom-12 md:bottom-20 right-8 md:right-16 text-lg md:text-2xl animate-pulse">
              🌀
            </div>
            <div className="absolute bottom-16 md:bottom-32 left-4 md:left-8 text-base md:text-xl animate-bounce delay-700">
              🏄‍♂️
            </div>
          </div>
        </div>

        {/* Recent Messages - 모바일 최적화 */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200">
          <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 md:p-6">
            <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
              <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
              <span className="font-bold">최근 메시지들 🌊</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 md:p-6">
            <div className="space-y-3 md:space-y-4">
              {Object.values(messages)
                .slice(-3)
                .map((message) => {
                  const decoInfo = getDecoInfo(message.decoType);
                  return (
                    <div
                      key={message.id}
                      className="flex items-start space-x-3 p-3 md:p-4 rounded-xl md:rounded-2xl bg-gradient-to-r from-cyan-50 to-blue-50 border border-cyan-200 md:border-2 transform hover:scale-105 transition-transform"
                    >
                      <div
                        className={`w-10 h-10 md:w-12 md:h-12 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center shadow-lg border-2 border-white flex-shrink-0`}
                      >
                        <span className="text-lg md:text-xl">
                          {decoInfo.emoji}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-gray-800 text-sm md:text-base font-medium leading-relaxed">
                          {message.content}
                        </p>
                        <div className="flex items-center space-x-2 mt-2">
                          <Badge
                            variant="outline"
                            className="text-xs bg-white border-cyan-300"
                          >
                            {message.anonymity}
                          </Badge>
                          <span className="text-xs text-gray-500 font-medium">
                            {decoInfo.name}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              {Object.keys(messages).length === 0 && (
                <div className="text-center py-8 md:py-12">
                  <div className="text-4xl md:text-6xl mb-4">📝</div>
                  <p className="text-gray-500 text-base md:text-lg font-semibold">
                    아직 메시지가 없어요
                  </p>
                  <p className="text-gray-400 text-xs md:text-sm mt-2 font-medium px-4">
                    친구들에게 롤링페이퍼를 공유해보세요! 💌
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    className="mt-4 bg-cyan-50 border-cyan-300 text-cyan-700 hover:bg-cyan-100"
                    onClick={handleShare}
                  >
                    <Share2 className="w-4 h-4 mr-2" />
                    지금 공유하기
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
