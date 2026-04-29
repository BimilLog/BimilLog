"use client";

import React from "react";
import Link from "next/link";
import { MessageSquare, Share2, List, MessageCircle, LogIn } from "lucide-react";
import { useRollingPaperShare } from "@/hooks/features/useRollingPaperShare";
import { useAuth, useToast } from "@/hooks";
import { shareRollingPaper, fallbackShare } from "@/lib/auth/kakao";
import { FriendActionButtons } from "./FriendActionButtons";

interface RollingPaperHeaderProps {
  nickname: string;
  messageCount: number;
  ownerId?: number | null;
  isOwner?: boolean;
  onShowMessages?: () => void;
  className?: string;
}

// 헤더 액션 버튼 공통 클래스: 최소 44x44 터치 타깃 + 일관된 스타일
const ACTION_BTN_BASE =
  "inline-flex items-center justify-center gap-1.5 min-w-[44px] min-h-[44px] px-3 rounded-lg text-sm font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed";

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = React.memo(({
  nickname,
  messageCount,
  ownerId,
  isOwner = false,
  onShowMessages,
  className = "",
}) => {
  const { isAuthenticated } = useAuth();
  const { showSuccess, showError } = useToast();
  const { handleWebShare } = useRollingPaperShare({
    nickname,
    messageCount,
    isOwner,
  });

  // 카카오 공유: 성공/실패와 무관하게 시도 결과를 토스트로 안내 (B-M2)
  const handleKakaoShare = async () => {
    if (!nickname) return;
    // 클릭 즉시 진행 안내 토스트 (사용자 피드백 우선) - 백그라운드 결과는 추가 토스트로 갱신
    showSuccess('공유 시작', '카카오톡으로 롤링페이퍼 공유를 시작했어요.');
    try {
      const success = await shareRollingPaper(nickname, messageCount);
      if (success) {
        showSuccess('공유 완료', '카카오톡으로 롤링페이퍼를 공유했어요.');
      } else {
        // SDK 미초기화/미지원 시 fallback 후 안내 토스트 노출
        const url =
          typeof window !== 'undefined'
            ? `${window.location.origin}/rolling-paper/${encodeURIComponent(nickname)}`
            : '';
        fallbackShare(
          url,
          `${nickname}님의 롤링페이퍼`,
          `${nickname}님에게 따뜻한 메시지를 남겨보세요!`,
        );
        showSuccess('공유 링크 복사', '카카오톡 공유가 어려워 링크를 복사했어요.');
      }
    } catch {
      showError('공유 실패', '카카오 공유 중 문제가 발생했어요.');
    }
  };

  return (
    <header
      data-testid="paper-header"
      className={`bg-white/80 backdrop-blur-md border-b ${className}`}
    >
      <div className="px-4 py-2.5">
        <div className="max-w-screen-xl mx-auto">
          {/* 단일 레이아웃: 모바일/데스크톱 모두 동일한 element 를 사용하고
              내부 텍스트만 viewport 에 따라 토글한다. (이중 testid 제거 + sticky 영역 단일화) */}
          <div className="flex items-center justify-between gap-2 md:gap-3 flex-wrap md:flex-nowrap">
            <div className="flex items-center space-x-2 flex-1 min-w-0">
              <div className="w-10 h-10 md:w-11 md:h-11 bg-brand-button rounded-lg flex items-center justify-center shadow-sm shrink-0">
                <MessageSquare className="w-5 h-5 stroke-white fill-white/30" aria-hidden="true" />
              </div>
              <div className="min-w-[44px] flex-1">
                <h1 className="font-bold text-gray-900 text-base md:text-xl truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0 flex-wrap justify-end">
              {onShowMessages && (
                <button
                  type="button"
                  data-testid="paper-header-action-list"
                  onClick={onShowMessages}
                  aria-label="메시지 목록 보기"
                  className={`${ACTION_BTN_BASE} bg-blue-50 text-blue-700 hover:bg-blue-100`}
                >
                  <List className="w-5 h-5" aria-hidden="true" />
                  <span className="hidden md:inline">메시지 목록</span>
                  <span className="md:hidden">목록</span>
                </button>
              )}
              {/* 친구 액션 영역: 인증 사용자에게만 노출 */}
              {isAuthenticated && (
                <div data-testid="paper-header-action-friend" className="flex items-center gap-2">
                  <FriendActionButtons nickname={nickname} ownerId={ownerId} isOwner={isOwner} />
                </div>
              )}
              {/* 비로그인 사용자용 로그인/친구요청 진입점 */}
              {!isAuthenticated && !isOwner && (
                <Link
                  href="/login"
                  data-testid="paper-header-login-cta"
                  aria-label="로그인하고 친구 요청 보내기"
                  className={`${ACTION_BTN_BASE} bg-purple-50 text-purple-700 hover:bg-purple-100`}
                >
                  <LogIn className="w-5 h-5" aria-hidden="true" />
                  <span className="hidden md:inline">로그인하고 친구 요청</span>
                  <span className="md:hidden">로그인</span>
                </Link>
              )}
              <button
                type="button"
                data-testid="paper-header-action-kakao"
                onClick={handleKakaoShare}
                aria-label="카카오톡으로 공유"
                className={`${ACTION_BTN_BASE} bg-yellow-400 text-gray-900 hover:bg-yellow-500`}
              >
                <MessageCircle className="w-5 h-5" aria-hidden="true" />
                <span className="hidden md:inline">카톡 공유</span>
                <span className="md:hidden">카톡</span>
              </button>
              <button
                type="button"
                data-testid="paper-header-action-link"
                onClick={handleWebShare}
                aria-label="링크 공유"
                className={`${ACTION_BTN_BASE} bg-gray-100 text-gray-700 hover:bg-gray-200`}
              >
                <Share2 className="w-5 h-5" aria-hidden="true" />
                <span className="hidden md:inline">링크 공유</span>
                <span className="md:hidden">링크</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
});

RollingPaperHeader.displayName = "RollingPaperHeader";
