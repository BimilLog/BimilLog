"use client";

import { useState } from "react";
import { Button } from "./button";
import { Share2 } from "lucide-react";
import {
  shareRollingPaper,
  sharePost,
  shareService,
  fallbackShare,
} from "@/lib/auth/kakao";

interface KakaoShareButtonProps {
  type: "service" | "rollingPaper" | "post";
  // 롤링페이퍼용
  userName?: string;
  messageCount?: number;
  // 게시글용
  postId?: number;
  title?: string;
  author?: string;
  content?: string;
  likes?: number;
  // 스타일
  variant?: "default" | "outline" | "ghost";
  size?: "default" | "sm" | "lg";
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
  variant = "default",
  size = "default",
  className,
}: KakaoShareButtonProps) {
  const [isSharing, setIsSharing] = useState(false);

  const handleShare = async () => {
    setIsSharing(true);

    try {
      let success = false;
      let fallbackUrl = "";
      let fallbackTitle = "";
      let fallbackText = "";

      switch (type) {
        case "service":
          success = await shareService();
          fallbackUrl = window.location.origin;
          fallbackTitle = "비밀로그";
          fallbackText =
            "익명 롤링페이퍼 서비스 - 친구들에게 따뜻한 메시지를 받아보세요!";
          break;

        case "rollingPaper":
          if (!userName) {
            console.error("롤링페이퍼 공유에는 userName이 필요합니다.");
            return;
          }
          success = await shareRollingPaper(userName, messageCount);
          fallbackUrl = `${
            window.location.origin
          }/rolling-paper/${encodeURIComponent(userName)}`;
          fallbackTitle = `${userName}님의 롤링페이퍼`;
          fallbackText = `${userName}님에게 따뜻한 메시지를 남겨보세요!`;
          break;

        case "post":
          if (!postId || !title || !author || !content) {
            console.error(
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
          console.error("지원하지 않는 공유 타입입니다.");
          return;
      }

      // 카카오톡 공유 실패 시 대체 공유 방법 사용
      if (!success) {
        fallbackShare(fallbackUrl, fallbackTitle, fallbackText);
      }
    } catch (error) {
      console.error("공유 중 오류 발생:", error);
    } finally {
      setIsSharing(false);
    }
  };

  return (
    <Button
      onClick={handleShare}
      disabled={isSharing}
      variant={variant}
      size={size}
      className={className}
    >
      <Share2 className="w-4 h-4 mr-2" />
      {isSharing ? "공유 중..." : type === "post" ? "공유" : "카카오톡 공유"}
    </Button>
  );
}
