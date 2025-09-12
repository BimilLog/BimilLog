"use client";

import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Search,
  Heart,
  AlertCircle,
  Mail,
  PartyPopper,
  Sparkles,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useRollingPaperSearch } from "@/hooks/useRollingPaperSearch";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";
import { RecentVisits } from "@/app/rolling-paper/components/RecentVisits";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export default function VisitClient() {
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const router = useRouter();
  
  const {
    searchNickname,
    setSearchNickname,
    isSearching,
    searchError,
    handleSearch,
    handleKeyPress,
  } = useRollingPaperSearch();

  const handleGoToMyRollingPaper = () => {
    setShowConfirmDialog(false);
    router.push("/rolling-paper");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />

      {/* Page Header - 모바일 최적화 */}
      <header className="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 sm:w-8 sm:h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center flex-shrink-0">
                <Heart className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
              </div>
              <h1 className="text-base sm:text-xl font-bold text-gray-800 whitespace-nowrap">
                롤링페이퍼 방문
              </h1>
            </div>
            <KakaoShareButton
              type="service"
              variant="outline"
              size="sm"
              className="px-2 sm:px-3 py-1 text-sm h-8"
            />
          </div>
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
                onChange={(e) => setSearchNickname(e.target.value)}
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

        {/* 최근 방문한 롤링페이퍼 */}
        <div className="mb-8">
          <RecentVisits />
        </div>

        {/* Info Section */}
        <div className="mt-8 text-center">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <Heart className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1 flex items-center space-x-2">
                  <Mail className="w-4 h-4" />
                  <span>익명으로 메시지를 남겨보세요!</span>
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
              <span>내 롤링페이퍼 발견!</span>
              <PartyPopper className="w-5 h-5" />
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
                  <Sparkles className="w-4 h-4 mr-1" />
                  내 롤링페이퍼 보기
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

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}
