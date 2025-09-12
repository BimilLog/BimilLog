"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/atoms/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/atoms/avatar";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/molecules/dialog";
import { Spinner } from "@/components/atoms/spinner";
import { EmptyState } from "@/components/molecules/empty-state";
import { Alert } from "@/components/molecules/alert";
import { userApi, KakaoFriendList } from "@/lib/api";
import { logoutAndRedirectToConsent } from "@/lib/kakao-auth";
import { Users, MessageCircle, X, RefreshCw, AlertCircle } from "lucide-react";
import { useRouter } from "next/navigation";

interface KakaoFriendsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function KakaoFriendsModal({ isOpen, onClose }: KakaoFriendsModalProps) {
  const [friendsData, setFriendsData] = useState<KakaoFriendList | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [needsConsent, setNeedsConsent] = useState(false);
  const router = useRouter();

  const fetchFriends = async () => {
    setIsLoading(true);
    setError(null);
    setNeedsConsent(false);
    try {
      const response = await userApi.getFriendList(0);

      if (response.success && response.data) {
        setFriendsData(response.data);
      } else {
        // 실패했지만 에러 메시지 체크
        const errorMessage =
          response?.error || "친구 목록을 가져올 수 없습니다.";

        // 카카오 친구 동의 실패 에러 체크
        if (errorMessage.includes("카카오 친구 추가 동의을 해야 합니다")) {
          setNeedsConsent(true);
          setError("카카오 친구 목록 접근을 위해 추가 동의가 필요합니다.");
        } else {
          setError(errorMessage);
        }
      }
    } catch (err) {
      console.error("카카오 친구 조회 예외:", err);
      setError("카카오 친구 조회 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleConsentClick = () => {
    try {
      // 확인 대화상자 표시
      const confirmed = window.confirm(
        "카카오 친구 목록 접근 권한을 받기 위해 잠시 로그아웃됩니다.\n" +
          "로그아웃 후 카카오 동의 페이지로 이동하시겠습니까?\n\n" +
          "동의 완료 후 자동으로 다시 로그인됩니다."
      );

      if (confirmed) {
        logoutAndRedirectToConsent();
      }
    } catch (err) {
      console.error("카카오 동의 처리 실패:", err);
      setError(
        err instanceof Error
          ? err.message
          : "동의 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchFriends();
    }
  }, [isOpen]);

  const handleVisitRollingPaper = (userName: string) => {
    if (userName) {
      router.push(`/rolling-paper/${encodeURIComponent(userName)}`);
      onClose();
    }
  };

  const getInitials = (nickname: string) => {
    return nickname ? nickname.substring(0, 1) : "?";
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md max-h-[80vh] p-0 overflow-hidden bg-white">
        {/* 카카오톡 스타일 헤더 */}
        <DialogHeader className="px-4 py-3 bg-gradient-to-r from-yellow-400 to-yellow-500 text-black">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center">
                <Users className="w-5 h-5 text-yellow-800" />
              </div>
              <div>
                <DialogTitle className="text-lg font-bold text-yellow-900">
                  카카오 친구
                </DialogTitle>
                {friendsData && (
                  <p className="text-sm text-yellow-800 opacity-90">
                    총 {friendsData.total_count}명
                  </p>
                )}
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Button
                variant="ghost"
                size="icon"
                onClick={fetchFriends}
                disabled={isLoading}
                className="h-8 w-8 text-yellow-800 hover:bg-yellow-300"
              >
                <RefreshCw
                  className={`h-4 w-4 ${isLoading ? "animate-spin" : ""}`}
                />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                onClick={onClose}
                className="h-8 w-8 text-yellow-800 hover:bg-yellow-300"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </DialogHeader>

        {/* 컨텐츠 영역 */}
        <div className="overflow-y-auto max-h-96">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12 px-4">
              <Spinner className="w-8 h-8 text-yellow-500 mb-4" />
              <p className="text-gray-600">친구 목록을 불러오는 중...</p>
            </div>
          ) : needsConsent ? (
            <div className="p-4">
              <Alert className="mb-4 border-yellow-200 bg-yellow-50">
                <AlertCircle className="h-4 w-4 text-yellow-600" />
                <div className="ml-2">
                  <h4 className="text-sm font-medium text-yellow-800 mb-1">
                    카카오 친구 목록 동의 필요
                  </h4>
                  <p className="text-sm text-yellow-700 mb-3">
                    카카오톡 친구 목록을 확인하려면 추가 동의가 필요합니다. 동의
                    페이지로 이동하여 &#39;친구 목록 제공&#39; 권한을 허용해주세요.
                  </p>
                  <div className="flex space-x-2">
                    <Button
                      onClick={handleConsentClick}
                      className="bg-yellow-500 hover:bg-yellow-600 text-white text-sm px-4 py-2"
                    >
                      동의하러 가기
                    </Button>
                    <Button
                      variant="outline"
                      onClick={fetchFriends}
                      className="text-sm px-4 py-2"
                    >
                      다시 시도
                    </Button>
                  </div>
                </div>
              </Alert>
            </div>
          ) : error ? (
            <div className="p-4">
              <EmptyState
                icon={<Users className="w-12 h-12" />}
                title="친구 목록을 불러올 수 없습니다"
                description={error}
                actionLabel="다시 시도"
                onAction={fetchFriends}
              />
            </div>
          ) : !friendsData || friendsData.elements.length === 0 ? (
            <div className="p-4">
              <EmptyState
                icon={<Users className="w-12 h-12" />}
                title="친구가 없습니다"
                description="카카오톡에서 친구를 추가하고 다시 시도해보세요"
                actionLabel="새로고침"
                onAction={fetchFriends}
              />
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {friendsData.elements.map((friend) => (
                <div
                  key={friend.id}
                  className="p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center space-x-3">
                    {/* 카카오톡 스타일 아바타 */}
                    <div className="relative">
                      <Avatar className="h-12 w-12 ring-2 ring-yellow-200">
                        <AvatarImage
                          src={friend.profile_thumbnail_image}
                          alt={friend.profile_nickname}
                        />
                        <AvatarFallback className="bg-yellow-100 text-yellow-800 font-semibold">
                          {getInitials(friend.profile_nickname)}
                        </AvatarFallback>
                      </Avatar>
                      {/* 온라인 상태 표시 (카카오톡 스타일) */}
                      <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-400 border-2 border-white rounded-full"></div>
                    </div>

                    {/* 친구 정보 */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <h3 className="text-sm font-semibold text-gray-900 truncate">
                          {friend.profile_nickname}
                        </h3>
                        {friend.userName && (
                          <span className="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded-full">
                            @{friend.userName}
                          </span>
                        )}
                      </div>
                      {friend.userName && (
                        <p className="text-xs text-gray-500 mt-1">
                          비밀로그 사용자
                        </p>
                      )}
                    </div>

                    {/* 액션 버튼 */}
                    <div className="flex items-center space-x-2">
                      {friend.userName ? (
                        <Button
                          size="sm"
                          onClick={() =>
                            handleVisitRollingPaper(friend.userName)
                          }
                          className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white text-xs px-3 py-1.5"
                        >
                          <MessageCircle className="w-3 h-3 mr-1" />
                          롤링페이퍼
                        </Button>
                      ) : (
                        <div className="text-xs text-gray-400">미가입</div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 푸터 */}
        {friendsData && friendsData.elements.length > 0 && (
          <div className="px-4 py-3 bg-gray-50 border-t">
            <p className="text-xs text-gray-500 text-center">
              비밀로그에 가입한 친구에게만 롤링페이퍼를 보낼 수 있습니다
            </p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
