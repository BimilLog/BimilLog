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
  ArrowLeft,
  Share2,
  Info,
  ChevronLeft,
  ChevronRight,
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
import { MessageView } from "./components/MessageView";
import {
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
  ResponsiveAdFitBanner,
} from "@/components";

export default function RollingPaperClient() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [messages, setMessages] = useState<{
    [key: number]: RollingPaperMessage;
  }>({});
  const [messageCount, setMessageCount] = useState(0);
  const [recentMessages, setRecentMessages] = useState<RollingPaperMessage[]>(
    []
  );
  const [isLoadingMessages, setIsLoadingMessages] = useState(true);
  const [isMobile, setIsMobile] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);

  const totalPages = isMobile ? 3 : 2;
  const colsPerPage = isMobile ? 4 : 6;
  const rowsPerPage = 10;
  const slotsPerPage = colsPerPage * rowsPerPage;

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkIsMobile();
    window.addEventListener("resize", checkIsMobile);

    return () => window.removeEventListener("resize", checkIsMobile);
  }, []);

  useEffect(() => {
    const fetchMessages = async () => {
      if (!isAuthenticated || !user) return;

      try {
        const response = await rollingPaperApi.getMyRollingPaper();
        if (response.success && response.data) {
          setMessageCount(response.data.length);
          setRecentMessages(response.data.slice(0, 3));

          const messageMap: { [key: number]: RollingPaperMessage } = {};

          // 백엔드 좌표를 항상 6열 기준으로 매핑 (일관성 유지)
          const FIXED_COLS_PER_PAGE = 6;

          response.data.forEach((message) => {
            // 백엔드 좌표를 6열 기준으로 해석
            const backendRow = message.height;
            const backendCol = message.width % FIXED_COLS_PER_PAGE;
            const backendPage =
              Math.floor(message.width / FIXED_COLS_PER_PAGE) + 1;

            // 현재 화면에서의 position 계산 (같은 페이지 내에서만)
            const currentPagePosition = backendRow * colsPerPage + backendCol;

            // 메시지에 페이지 정보 추가 (원본 좌표도 보존)
            const messageWithPage = {
              ...message,
              page: backendPage,
              currentPosition: currentPagePosition,
              originalWidth: message.width,
              originalHeight: message.height,
            };
            messageMap[currentPagePosition] = messageWithPage;
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
          text: `${user.userName}님에게 따뜻한 메시지를 남겨보세요!`,
          url: url,
        });
      } catch (error) {
        console.log("Share cancelled or failed", error);
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
        alert("링크가 클립보드에 복사되었습니다!");
      } catch (clipboardError) {
        console.error("클립보드 복사 실패:", clipboardError);
        alert("링크 복사에 실패했습니다.");
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      <AuthHeader />

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
                  <p className="text-xs text-gray-500">총 {messageCount}개</p>
                </div>
              </div>
            </div>
            <div className="flex items-center space-x-1 md:space-x-2">
              <KakaoShareButton
                type="rollingPaper"
                userName={user.userName}
                messageCount={messageCount}
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
            </div>
          </div>
        </div>
      </header>

      {/* Top Banner Advertisement */}
      <div className="container mx-auto px-4 py-2">
        <div className="flex justify-center">
          <ResponsiveAdFitBanner
            position="내 롤링페이퍼 상단"
            className="max-w-full"
          />
        </div>
      </div>

      <div className="container mx-auto px-2 md:px-4 py-4 md:py-8">
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

        <div className="relative max-w-5xl mx-auto mb-6 md:mb-8">
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

            <div className="pt-6 md:pt-8 pb-4 md:pb-6 px-12 md:px-20 text-center">
              <div className="relative">
                <h1 className="text-lg md:text-3xl font-bold text-cyan-800 mb-2 transform -rotate-1">
                  🌊 {user.userName}님의 롤링페이퍼 🌊
                </h1>
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
                  총 {messageCount}개의 시원한 메시지 💌
                </p>
              </div>
            </div>

            <div className="px-12 md:px-20 pb-4 md:pb-6">
              <div className="flex justify-center items-center mb-4 space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                  disabled={currentPage === 1}
                  className="h-8 w-8 p-0 bg-white/80"
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>

                <div className="flex space-x-1">
                  {Array.from({ length: totalPages }, (_, i) => (
                    <Button
                      key={i + 1}
                      variant={currentPage === i + 1 ? "default" : "outline"}
                      size="sm"
                      onClick={() => setCurrentPage(i + 1)}
                      className={`h-8 w-8 p-0 ${
                        currentPage === i + 1
                          ? "bg-gradient-to-r from-blue-500 to-cyan-600 text-white"
                          : "bg-white/80"
                      }`}
                    >
                      {i + 1}
                    </Button>
                  ))}
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setCurrentPage(Math.min(totalPages, currentPage + 1))
                  }
                  disabled={currentPage === totalPages}
                  className="h-8 w-8 p-0 bg-white/80"
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>

              <div className="text-center mb-4">
                <p className="text-cyan-600 text-sm font-medium">
                  페이지 {currentPage} / {totalPages}
                </p>
              </div>

              <div className="grid grid-cols-4 md:grid-cols-6 gap-2 md:gap-3 bg-white/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-cyan-300">
                {Array.from({ length: slotsPerPage }, (_, i) => {
                  // 현재 페이지의 메시지만 필터링 (6열 기준으로 페이지 계산)
                  const FIXED_COLS_PER_PAGE = 6;
                  const pageMessages = Object.entries(messages).filter(
                    ([_, message]) => {
                      const messagePage =
                        Math.floor(message.width / FIXED_COLS_PER_PAGE) + 1;
                      return messagePage === currentPage;
                    }
                  );

                  // 현재 슬롯에 해당하는 메시지 찾기
                  const slotMessage = pageMessages.find(([_, message]) => {
                    const backendRow = message.height;
                    const backendCol = message.width % FIXED_COLS_PER_PAGE;
                    const currentPagePosition =
                      backendRow * colsPerPage + backendCol;
                    return currentPagePosition === i;
                  });

                  const hasMessage = slotMessage ? slotMessage[1] : null;
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
                              <div className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-1.5 h-1.5 md:w-2 md:h-2 bg-yellow-300 rounded-full animate-ping"></div>
                            </div>
                          ) : (
                            <div className="text-cyan-300 text-xs md:text-sm text-center leading-tight opacity-0"></div>
                          )}
                        </div>
                      </DialogTrigger>
                      {hasMessage && (
                        <DialogContent className="max-w-sm md:max-w-md mx-auto bg-gradient-to-br from-cyan-50 to-blue-50 border-2 md:border-4 border-cyan-200 rounded-2xl md:rounded-3xl">
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

        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200">
          <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 md:p-6">
            <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
              <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
              <span className="font-bold">최근 메시지들 🌊</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 md:p-6">
            <div className="space-y-3 md:space-y-4">
              {recentMessages.map((message) => {
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
              {messageCount === 0 && (
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

        {/* Mobile Advertisement */}
        <div className="mt-6 mb-4">
          <div className="flex justify-center px-2">
            {(() => {
              const adUnit = getAdUnit("MOBILE_BANNER");
              return adUnit ? (
                <AdFitBanner
                  adUnit={adUnit}
                  width={AD_SIZES.BANNER_320x50.width}
                  height={AD_SIZES.BANNER_320x50.height}
                  onAdFail={() =>
                    console.log("자신의 롤링페이퍼 페이지 광고 로딩 실패")
                  }
                />
              ) : null;
            })()}
          </div>
        </div>
      </div>
    </div>
  );
}
