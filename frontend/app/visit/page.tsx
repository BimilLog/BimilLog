"use client";

import type React from "react";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Search,
  ArrowLeft,
  Heart,
  Users,
  AlertCircle,
  CheckCircle,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { rollingPaperApi } from "@/lib/api";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export default function VisitPage() {
  const { user } = useAuth();
  const [searchNickname, setSearchNickname] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const router = useRouter();

  const handleSearch = async () => {
    if (!searchNickname.trim()) return;

    const trimmedNickname = searchNickname.trim();
    setIsSearching(true);
    setSearchError("");

    try {
      // 자신의 닉네임인지 확인
      if (user && user.userName === trimmedNickname) {
        setShowConfirmDialog(true);
        setIsSearching(false);
        return;
      }

      // 다른 사용자의 롤링페이퍼 조회 시도
      const response = await rollingPaperApi.getRollingPaper(trimmedNickname);

      if (response.success) {
        // 성공적으로 조회된 경우 방문 페이지로 이동
        router.push(`/rolling-paper/${encodeURIComponent(trimmedNickname)}`);
      } else {
        // 사용자를 찾을 수 없는 경우
        if (
          response.error &&
          response.error.includes("사용자를 찾을 수 없습니다")
        ) {
          setSearchError(
            "해당 닉네임의 롤링페이퍼를 찾을 수 없어요. 회원가입한 사용자의 롤링페이퍼만 존재합니다."
          );
        } else {
          setSearchError(response.error || "롤링페이퍼를 찾을 수 없어요.");
        }
      }
    } catch (error) {
      console.error("Search error:", error);
      setSearchError("검색 중 오류가 발생했어요. 다시 시도해 주세요.");
    } finally {
      setIsSearching(false);
    }
  };

  const handleGoToMyRollingPaper = () => {
    setShowConfirmDialog(false);
    router.push("/rolling-paper");
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  // 인기 롤링페이퍼 예시 데이터
  const popularRollingPapers = [
    { nickname: "행복한토끼", messageCount: 47, emoji: "🐰" },
    { nickname: "별빛소녀", messageCount: 32, emoji: "⭐" },
    { nickname: "코딩마스터", messageCount: 28, emoji: "💻" },
    { nickname: "꽃길만걷자", messageCount: 25, emoji: "🌸" },
    { nickname: "햇살같은사람", messageCount: 23, emoji: "☀️" },
    { nickname: "달콤한하루", messageCount: 19, emoji: "🍯" },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                홈으로
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                <Heart className="w-5 h-5 text-white" />
              </div>
              <h1 className="text-xl font-bold text-gray-800">
                롤링페이퍼 방문
              </h1>
            </div>
          </div>
          <KakaoShareButton
            type="service"
            variant="outline"
            size="sm"
            className="px-3 py-1 text-sm h-8"
          />
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-md">
        {/* Search Section */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
          <CardHeader className="text-center pb-4">
            <CardTitle className="text-2xl bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent">
              누구의 롤링페이퍼를 방문할까요?
            </CardTitle>
            <p className="text-gray-600 text-sm">
              닉네임을 입력하여 롤링페이퍼를 찾아보세요
            </p>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <Input
                placeholder="닉네임을 입력하세요"
                value={searchNickname}
                onChange={(e) => {
                  setSearchNickname(e.target.value);
                  if (searchError) setSearchError("");
                }}
                onKeyPress={handleKeyPress}
                className="pl-10 h-12 text-lg bg-white border-2 border-gray-200 focus:border-purple-400"
                disabled={isSearching}
              />
            </div>

            {/* 검색 오류 메시지 */}
            {searchError && (
              <div className="flex items-start space-x-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                <div className="text-sm text-red-800">
                  <p className="font-medium mb-1">
                    롤링페이퍼를 찾을 수 없어요
                  </p>
                  <p>{searchError}</p>
                </div>
              </div>
            )}

            <Button
              onClick={handleSearch}
              className="w-full h-12 bg-gradient-to-r from-blue-500 to-cyan-600 hover:from-blue-600 hover:to-cyan-700 text-lg font-semibold disabled:opacity-50"
              disabled={!searchNickname.trim() || isSearching}
            >
              {isSearching ? (
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>검색 중...</span>
                </div>
              ) : (
                "롤링페이퍼 방문하기"
              )}
            </Button>
          </CardContent>
        </Card>

        {/* Popular Rolling Papers */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2 text-lg">
              <Users className="w-5 h-5 text-orange-500" />
              <span>인기 롤링페이퍼</span>
              <span className="text-xs bg-orange-100 text-orange-600 px-2 py-1 rounded-full">
                예시
              </span>
            </CardTitle>
            <p className="text-gray-600 text-sm">
              참고용 예시 데이터입니다. 실제 사용자의 닉네임을 입력해 주세요.
            </p>
          </CardHeader>
          <CardContent className="space-y-3">
            {popularRollingPapers.map((paper, index) => (
              <div
                key={paper.nickname}
                className="flex items-center justify-between p-3 bg-gradient-to-r from-gray-50 to-white rounded-lg border border-gray-100 hover:shadow-md transition-all cursor-pointer"
                onClick={async () => {
                  setSearchNickname(paper.nickname);
                  // 인기 롤링페이퍼는 예시 데이터이므로 별도 처리
                  setSearchError("");
                  setIsSearching(true);

                  try {
                    const response = await rollingPaperApi.getRollingPaper(
                      paper.nickname
                    );
                    if (response.success) {
                      router.push(
                        `/rolling-paper/${encodeURIComponent(paper.nickname)}`
                      );
                    } else {
                      setSearchError(
                        "죄송해요, 해당 롤링페이퍼는 예시 데이터입니다. 실제 사용자의 닉네임을 검색해 주세요."
                      );
                    }
                  } catch (error) {
                    setSearchError(
                      "죄송해요, 해당 롤링페이퍼는 예시 데이터입니다. 실제 사용자의 닉네임을 검색해 주세요."
                    );
                  } finally {
                    setIsSearching(false);
                  }
                }}
              >
                <div className="flex items-center space-x-3">
                  <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-r from-cyan-100 to-blue-100 rounded-full">
                    <span className="text-lg">{paper.emoji}</span>
                  </div>
                  <div>
                    <h3 className="font-medium text-gray-800">
                      {paper.nickname}
                    </h3>
                    <p className="text-xs text-gray-500">
                      {paper.messageCount}개의 메시지
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-1">
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white ${
                      index === 0
                        ? "bg-yellow-500"
                        : index === 1
                        ? "bg-gray-400"
                        : index === 2
                        ? "bg-orange-400"
                        : "bg-purple-400"
                    }`}
                  >
                    {index + 1}
                  </div>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Info Section */}
        <div className="mt-8 text-center">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <Heart className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1">
                  💌 익명으로 메시지를 남겨보세요!
                </p>
                <p>
                  로그인 없이도 누구나 따뜻한 메시지를 남길 수 있어요. 다양한
                  귀여운 디자인으로 메시지를 꾸며보세요!
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 자신의 롤링페이퍼 확인 다이얼로그 */}
      <Dialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <DialogContent className="max-w-sm mx-4 bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-center text-blue-800 font-bold flex items-center justify-center space-x-2">
              <CheckCircle className="w-5 h-5" />
              <span>내 롤링페이퍼 발견! 🎉</span>
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 p-2">
            <div className="text-center">
              <p className="text-blue-700 text-sm mb-4">
                입력하신 닉네임은 본인의 롤링페이퍼입니다!
                <br />내 롤링페이퍼로 이동하시겠어요?
              </p>

              <div className="space-y-3">
                <Button
                  onClick={handleGoToMyRollingPaper}
                  className="w-full bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white font-semibold py-2 px-4 rounded-xl"
                >
                  ✨ 내 롤링페이퍼 보기
                </Button>

                <Button
                  variant="outline"
                  onClick={() => setShowConfirmDialog(false)}
                  className="w-full border-blue-300 text-blue-700 hover:bg-blue-50 py-2 px-4 rounded-xl"
                >
                  다른 롤링페이퍼 찾기
                </Button>
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
