"use client";

import { useState, useEffect } from "react";
import dynamic from "next/dynamic";
import { Modal, ModalHeader, ModalBody, Spinner as FlowbiteSpinner } from "flowbite-react";
import { Button } from "@/components";
import { Avatar } from "flowbite-react";
import { getInitials } from "@/lib/utils/format";
import {
  Spinner,
  EmptyState,
  Alert
} from "@/components";
import { userQuery, KakaoFriendList } from "@/lib/api";
import { logger } from '@/lib/utils/logger';
import { logoutAndRedirectToConsent } from "@/lib/auth/kakao";
import { Users, MessageCircle, RefreshCw, AlertCircle } from "lucide-react";
import { useRouter } from "next/navigation";

interface KakaoFriendsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

/**
 * 카카오 친구 모달 로딩 컴포넌트
 * Dynamic import 중 카카오톡 스타일의 로딩 UI를 표시
 */
const KakaoFriendsModalLoading = () => (
  <Modal show onClose={() => {}} size="md">
    <ModalHeader className="bg-gradient-to-r from-yellow-400 to-yellow-500">
      <div className="flex items-center space-x-2">
        <div className="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center">
          <Users className="w-5 h-5 text-yellow-800" />
        </div>
        <span className="text-lg font-bold text-yellow-900">
          카카오 친구
        </span>
      </div>
    </ModalHeader>
    <ModalBody>
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="flex flex-col items-center gap-3">
          <Spinner size="lg" />
          <p className="text-sm text-brand-secondary">카카오 친구 목록 로딩 중...</p>
        </div>
      </div>
    </ModalBody>
  </Modal>
);

/**
 * 카카오 친구 모달 메인 컴포넌트
 * 카카오톡 친구 목록을 조회하고 비밀로그 회원인 친구들의 롤링페이퍼로 이동 가능
 * 카카오 API 에러 처리 및 동의 플로우 관리 포함
 */
function KakaoFriendsModalContent({ isOpen, onClose }: KakaoFriendsModalProps) {
  // 카카오 친구 목록 데이터 (API 응답 전체 객체)
  const [friendsData, setFriendsData] = useState<KakaoFriendList | null>(null);

  // UI 상태 관리 (로딩, 에러, 동의 필요 여부)
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 카카오 친구 목록 접근 동의가 필요한 상태 (scope 추가 필요)
  const [needsConsent, setNeedsConsent] = useState(false);

  // Next.js 라우터 (친구 롤링페이퍼 페이지 이동용)
  const router = useRouter();

  /**
   * 카카오 친구 목록 조회 함수
   * 카카오 API 호출하여 친구 목록을 가져오고 에러 상황별로 적절한 상태 설정
   * 동의 필요 에러의 경우 별도 플래그로 관리하여 동의 UI 표시
   */
  const fetchFriends = async () => {
    setIsLoading(true);
    setError(null);
    setNeedsConsent(false);
    try {
      // 카카오 친구 API 호출 (offset: 0부터 시작)
      const response = await userQuery.getFriendList(0);

      if (response.success && response.data) {
        setFriendsData(response.data);
      } else {
        // API 실패 시 에러 메시지 분석
        const errorMessage =
          response?.error || "친구 목록을 가져올 수 없습니다.";

        // 카카오 친구 목록 접근 동의가 필요한 경우 감지
        const normalizedMessage = errorMessage.replace(/\s/g, "");
        const consentKeywords = [
          "카카오친구추가동의를해야합니다",
        ];

        if (consentKeywords.some((keyword) => normalizedMessage.includes(keyword))) {
          setNeedsConsent(true);
          setError("카카오 친구 목록 접근을 위해 추가 동의가 필요합니다.");
        } else {
          setError(errorMessage);
        }
      }
    } catch (err) {
      logger.error("카카오 친구 조회 예외:", err);
      setError("카카오 친구 조회 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 카카오 친구 목록 동의 처리 함수
   * 사용자 확인 후 로그아웃 → 카카오 동의 페이지로 리다이렉트
   * 동의 완료 후 자동으로 다시 로그인되는 플로우
   */
  const handleConsentClick = () => {
    try {
      // 사용자에게 동의 프로세스 안내
      const confirmed = window.confirm(
        "카카오 친구 목록 접근 권한을 받기 위해 잠시 로그아웃됩니다.\n" +
          "로그아웃 후 카카오 동의 페이지로 이동하시겠습니까?\n\n" +
          "동의 완료 후 자동으로 다시 로그인됩니다."
      );

      if (confirmed) {
        logoutAndRedirectToConsent();
      }
    } catch (err) {
      logger.error("카카오 동의 처리 실패:", err);
      setError(
        err instanceof Error
          ? err.message
          : "동의 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
    }
  };

  // 모달이 열릴 때 친구 목록 자동 조회 (의존성: isOpen)
  useEffect(() => {
    if (isOpen) {
      fetchFriends();
    }
  }, [isOpen]);

  /**
   * 친구의 롤링페이퍼 페이지로 이동하는 핸들러
   * memberName을 URL 인코딩하여 안전한 라우팅 보장
   */
  const handleVisitRollingPaper = (memberName: string) => {
    if (memberName) {
      // 친구 롤링페이퍼 페이지로 라우팅 후 모달 닫기
      router.push(`/rolling-paper/${encodeURIComponent(memberName)}`);
      onClose();
    }
  };


  return (
    <Modal show={isOpen} onClose={onClose} size="md">
      <ModalHeader className="bg-gradient-to-r from-yellow-400 to-yellow-500">
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center">
              <Users className="w-5 h-5 text-yellow-800" />
            </div>
            <div>
              <span className="text-lg font-bold text-yellow-900">
                카카오 친구
              </span>
              {/* 친구 총 개수 표시 */}
              {friendsData && (
                <p className="text-sm text-yellow-800 opacity-90">
                  총 {friendsData.total_count}명
                </p>
              )}
            </div>
          </div>
          {/* 새로고침 버튼 */}
          <div className="flex items-center">
            <Button
              variant="ghost"
              size="icon"
              onClick={fetchFriends}
              disabled={isLoading}
              className="h-8 w-8 text-yellow-800 hover:bg-yellow-300"
            >
              {isLoading ? (
                <FlowbiteSpinner color="yellow" size="xs" aria-label="새로고침 중..." />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
            </Button>
          </div>
        </div>
      </ModalHeader>
      <ModalBody className="p-0">

        {/* 메인 컨텐츠 영역 - 상태별 조건부 렌더링 */}
        <div className="overflow-y-auto max-h-96">
          {isLoading ? (
            /* 로딩 상태 */
            <div className="flex flex-col items-center justify-center py-12 px-4">
              <Spinner className="w-8 h-8 text-yellow-500 mb-4" />
              <p className="text-brand-muted">친구 목록을 불러오는 중...</p>
            </div>
          ) : needsConsent ? (
            /* 카카오 친구 목록 접근 동의가 필요한 상태 */
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
            /* 에러 상태 - 일반적인 API 에러 */
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
            /* 빈 상태 - 친구가 없는 경우 */
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
            /* 친구 목록 표시 - 각 친구별 카드 형태로 렌더링 */
            <div className="divide-y divide-gray-100">
              {friendsData.elements.map((friend) => (
                <div
                  key={friend.id}
                  className="p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center space-x-3">
                    {/* 카카오톡 스타일 아바타 */}
                    <div className="relative">
                      <Avatar
                        img={friend.profile_thumbnail_image}
                        alt={friend.profile_nickname}
                        placeholderInitials={getInitials(friend.profile_nickname)}
                        className="h-12 w-12 ring-2 ring-yellow-200"
                        size="md"
                        rounded
                      />
                    </div>

                    {/* 친구 정보 (닉네임, 비밀로그 가입 여부) */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <h3 className="text-sm font-semibold text-brand-primary truncate">
                          {friend.profile_nickname}
                        </h3>
                        {/* 비밀로그 가입 여부 표시 (memberName이 있으면 가입자) */}
                        {friend.memberName && (
                          <span className="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded-full">
                            @{friend.memberName}
                          </span>
                        )}
                      </div>
                      {friend.memberName && (
                        <p className="text-xs text-brand-secondary mt-1">
                          비밀로그 사용자
                        </p>
                      )}
                    </div>

                    {/* 액션 버튼 영역 (가입자/비가입자 구분) */}
                    <div className="flex items-center space-x-2">
                      {friend.memberName ? (
                        /* 비밀로그 회원인 경우 롤링페이퍼 바로가기 버튼 */
                        <Button
                          size="sm"
                          onClick={() =>
                            handleVisitRollingPaper(friend.memberName)
                          }
                          className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white text-xs px-3 py-1.5"
                        >
                          <MessageCircle className="w-3 h-3 mr-1" />
                          롤링페이퍼
                        </Button>
                      ) : (
                        /* 비가입 회원 표시 (클릭 불가) */
                        <div className="text-xs text-brand-secondary">미가입</div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 푸터 - 사용법 안내 */}
        {friendsData && friendsData.elements.length > 0 && (
          <div className="px-4 py-3 bg-gray-50 border-t">
            <p className="text-xs text-brand-secondary text-center">
              비밀로그에 가입한 친구에게만 롤링페이퍼를 보낼 수 있습니다
            </p>
          </div>
        )}
      </ModalBody>
    </Modal>
  );
}

/**
 * 카카오 친구 모달 컴포넌트 (Dynamic Import)
 * SSR 환경에서 카카오 API 호출 문제를 방지하기 위해
 * 클라이언트 사이드에서만 렌더링되도록 설정
 */
const KakaoFriendsModal = dynamic(
  () => Promise.resolve(KakaoFriendsModalContent),
  {
    ssr: false,
    loading: () => <KakaoFriendsModalLoading />,
  }
);

export { KakaoFriendsModal };
