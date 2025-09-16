"use client";

import { useState } from "react";
import { Button } from "flowbite-react";
import { MessageCircle } from "lucide-react";
import {
  shareRollingPaper,
  sharePost,
  shareService,
  fallbackShare,
} from "@/lib/auth/kakao";
import { logger } from '@/lib/utils/logger';

/**
 * 카카오톡 공유 버튼 컴포넌트
 * - 서비스 공유, 롤링페이퍼 공유, 게시글 공유 3가지 타입을 지원
 * - 카카오톡 앱 설치 여부와 브라우저 호환성을 체크하여 자동으로 fallback 처리
 * - 각 공유 타입별로 필요한 데이터가 다름에 주의
 */
/**
 * 카카오톡 공유 버튼 Props 인터페이스
 *
 * 3가지 공유 타입별 필수 데이터:
 * - service: 추가 데이터 불필요 (메인 서비스 소개)
 * - rollingPaper: userName 필수, messageCount 선택
 * - post: postId, title, author, content 필수, likes 선택
 */
interface KakaoShareButtonProps {
  type: "service" | "rollingPaper" | "post";
  // 롤링페이퍼용 - userName은 필수, messageCount는 공유 메시지에 포함
  userName?: string;
  messageCount?: number;
  // 게시글용 - postId, title, author, content는 필수, likes는 선택
  postId?: number;
  title?: string;
  author?: string;
  content?: string;
  likes?: number;
  // 스타일
  color?: "blue" | "gray" | "dark" | "light" | "green" | "red" | "yellow" | "purple";
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  className?: string;
}

export function KakaoShareButton({
  type,
  userName,
  messageCount = 0,
  postId,
  title,
  author,
  content,
  likes = 0,
  color = "yellow",
  size = "sm",
  className,
}: KakaoShareButtonProps) {
  // 공유 진행 상태 관리 - 중복 클릭 방지
  const [isSharing, setIsSharing] = useState(false);

  /**
   * 공유 처리 메인 로직
   *
   * 처리 흐름:
   * 1. 카카오톡 SDK를 통한 1차 공유 시도
   * 2. 실패 시 브라우저 Web Share API 사용 (iOS Safari, Android Chrome 등)
   * 3. 최종 실패 시 URL 클립보드 복사
   *
   * 각 타입별 필수 데이터 검증 후 적절한 공유 함수 호출
   */
  const handleShare = async () => {
    setIsSharing(true);

    try {
      // 공유 타입별로 다른 처리 결과와 fallback 데이터를 준비
      let success = false;
      let fallbackUrl = "";    // 카카오톡 공유 실패 시 웹 공유 URL
      let fallbackTitle = "";  // 웹 공유 API용 제목
      let fallbackText = "";   // 웹 공유 API용 설명 텍스트

      // 공유 타입별 분기 처리 - 각 타입마다 필요한 데이터와 공유 로직이 다름
      switch (type) {
        case "service":
          // 서비스 전체 소개 공유 (메인 페이지 등에서 사용)
          success = await shareService();
          fallbackUrl = window.location.origin;
          fallbackTitle = "비밀로그";
          fallbackText =
            "익명 롤링페이퍼 서비스 - 친구들에게 따뜻한 메시지를 받아보세요!";
          break;

        case "rollingPaper":
          // 특정 사용자의 롤링페이퍼 공유 - userName 필수 검증
          if (!userName) {
            logger.error("롤링페이퍼 공유에는 userName이 필요합니다.");
            return;
          }
          success = await shareRollingPaper(userName, messageCount);
          // URL 인코딩으로 특수문자가 포함된 사용자명 처리 (한글, 공백, 특수문자 대응)
          fallbackUrl = `${
            window.location.origin
          }/rolling-paper/${encodeURIComponent(userName)}`;
          fallbackTitle = `${userName}님의 롤링페이퍼`;
          fallbackText = `${userName}님에게 따뜻한 메시지를 남겨보세요!`;
          break;

        case "post":
          // 커뮤니티 게시글 공유 - 모든 필수 데이터 검증
          if (!postId || !title || !author || !content) {
            logger.error(
              "게시글 공유에는 postId, title, author, content가 필요합니다."
            );
            return;
          }
          success = await sharePost(postId, title, author, content, likes);
          fallbackUrl = `${window.location.origin}/board/post/${postId}`;
          fallbackTitle = title;
          fallbackText = `${author}님이 작성한 글`;
          break;

        default:
          logger.error("지원하지 않는 공유 타입입니다.");
          return;
      }

      // 카카오톡 SDK 실패 시 브라우저의 Web Share API나 클립보드 복사로 fallback
      // iOS Safari, Android Chrome 등에서는 Web Share API 지원
      // 지원하지 않는 브라우저에서는 클립보드에 URL 복사 후 사용자에게 알림
      if (!success) {
        fallbackShare(fallbackUrl, fallbackTitle, fallbackText);
      }
    } catch (error) {
      // 예상치 못한 에러 (네트워크, SDK 로딩 실패 등) 처리
      logger.error("공유 중 오류 발생:", error);
    } finally {
      // 성공/실패와 관계없이 로딩 상태 해제
      setIsSharing(false);
    }
  };

  return (
    <Button
      onClick={handleShare}
      disabled={isSharing}  // 중복 클릭 방지
      color={color}
      size={size}
      className={`!bg-yellow-400 !hover:bg-yellow-500 !text-white ${className}`}
    >
      <MessageCircle className="w-4 h-4 mr-2" />
      {/* 상태별 버튼 텍스트 - 로딩 중이거나 게시글 타입에 따라 다르게 표시 */}
      {isSharing ? "공유 중..." : type === "post" ? "공유" : "카카오톡 공유"}
    </Button>
  );
}
