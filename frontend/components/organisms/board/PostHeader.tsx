"use client";

import React, { useCallback } from "react";
import { useToast } from "@/hooks";
import { Button, CardHeader, CardTitle, Badge } from "@/components";
import {
  Eye,
  ThumbsUp,
  MessageSquare,
  Lock,
  User,
  ExternalLink,
  Share2,
  Megaphone,
} from "lucide-react";
import { Post } from "@/lib/api";
import Link from "next/link";
import { KakaoShareButton } from "@/components";
import { Popover, Button as FlowbiteButton } from "flowbite-react";
import { formatDateTime } from "@/lib/utils/date";

interface PostHeaderProps {
  post: Post;
  commentCount: number;
}

export const PostHeader = React.memo<PostHeaderProps>(({
  post,
  commentCount,
}) => {
  const { showSuccess, showError } = useToast();

  // 링크 공유 기능 (롤링페이퍼와 동일한 로직)
  const handleWebShare = useCallback(async () => {
    const shareData = {
      title: `${post.title} | 비밀로그`,
      text: `${post.memberName || "익명"}님이 작성한 글`,
      url: `${window.location.origin}/board/post/${post.id}`,
    };

    // 네이티브 공유 API 지원 여부 확인
    if (navigator.share && navigator.canShare && navigator.canShare(shareData)) {
      try {
        await navigator.share(shareData);
        showSuccess('공유 완료', '게시글 링크가 공유되었습니다.');
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시
        if ((error as Error).name !== 'AbortError') {
          showError('공유 실패', '공유하기에 실패했습니다.');
        }
      }
    } else {
      // 클립보드 복사 폴백
      try {
        await navigator.clipboard.writeText(shareData.url);
        showSuccess('복사 완료', '링크가 클립보드에 복사되었습니다.');
      } catch {
        showError('복사 실패', '링크 복사에 실패했습니다.');
      }
    }
  }, [post.id, post.title, post.memberName, showSuccess, showError]);

  return (
    <CardHeader className="border-b p-4 md:p-6">
      {/* 제목 */}
      <div className="mb-4">
        <div className="flex items-center gap-3 mb-3">
          {post.password && (
            <Lock className="w-4 h-4 stroke-red-500 fill-red-100" />
          )}
          {post.isNotice && (
            <Badge variant="info" icon={Megaphone}>공지</Badge>
          )}
        </div>
        <CardTitle className="text-xl md:text-2xl font-bold text-brand-primary leading-tight">
          {post.title}
        </CardTitle>
      </div>

      {/* 작성자 정보 - 모바일 최적화 */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-col space-y-2">
          {/* 작성자와 시간 */}
          <div className="flex items-center space-x-3 text-sm text-brand-muted">
            <div className="flex items-center space-x-2 min-w-0">
              {post.memberName && post.memberName !== "익명" ? (
                <Popover
                  trigger="click"
                  placement="bottom"
                  content={
                    <div className="p-3 w-56">
                      <div className="flex flex-col space-y-2">
                        <div className="flex items-center space-x-2">
                          <User className="w-4 h-4 stroke-slate-600 fill-slate-100" />
                          <span className="font-medium">{post.memberName}</span>
                        </div>
                        <Link
                          href={`/rolling-paper/${encodeURIComponent(
                            post.memberName
                          )}`}
                        >
                          <Button size="sm" className="w-full justify-start">
                            <ExternalLink className="w-4 h-4 mr-2 stroke-blue-600 fill-blue-100" />
                            롤링페이퍼 보기
                          </Button>
                        </Link>
                      </div>
                    </div>
                  }
                >
                  <button className="truncate max-w-[120px] md:max-w-none hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1">
                    <User className="w-3 h-3 stroke-slate-600 fill-slate-100" />
                    <span>{post.memberName}</span>
                  </button>
                </Popover>
              ) : (
                <span className="truncate max-w-[120px] md:max-w-none text-brand-secondary">
                  {post.memberName || "익명"}
                </span>
              )}
            </div>
            <span className="text-xs text-brand-secondary whitespace-nowrap">
              {formatDateTime(post.createdAt)}
            </span>
          </div>

          {/* 통계 정보 */}
          <div className="flex items-center space-x-4 text-sm text-brand-muted">
            <div className="flex items-center space-x-1">
              <Eye className="w-4 h-4 stroke-purple-600 fill-purple-100" />
              <span>{post.viewCount}</span>
            </div>
            <div className="flex items-center space-x-1">
              <ThumbsUp className="w-4 h-4 stroke-blue-500 fill-blue-100" />
              <span>{post.likeCount}</span>
            </div>
            <div className="flex items-center space-x-1">
              <MessageSquare className="w-4 h-4 stroke-blue-600 fill-blue-100" />
              <span>{commentCount}</span>
            </div>
          </div>
        </div>

        {/* 버튼 영역 - 모바일 최적화 */}
        <div className="flex flex-col sm:flex-row gap-2 sm:items-center">
          {/* 카카오톡 공유 버튼 */}
          <KakaoShareButton
            type="post"
            postId={post.id}
            title={post.title}
            author={post.memberName || "익명"}
            content={post.content}
            likes={post.likeCount}
            size="sm"
            className="w-full sm:w-auto"
          />
          {/* 링크 공유 버튼 (롤링페이퍼와 동일한 디자인) */}
          <FlowbiteButton
            onClick={handleWebShare}
            color="gray"
            size="sm"
            className="text-xs"
          >
            <Share2 className="w-4 h-4 mr-1" />
            링크 공유
          </FlowbiteButton>
        </div>
      </div>
    </CardHeader>
  );
}, (prevProps, nextProps) => {
  // Post 객체의 핵심 필드들과 commentCount 비교
  return (
    prevProps.post.id === nextProps.post.id &&
    prevProps.post.title === nextProps.post.title &&
    prevProps.post.viewCount === nextProps.post.viewCount &&
    prevProps.post.likeCount === nextProps.post.likeCount &&
    prevProps.commentCount === nextProps.commentCount
  );
});

PostHeader.displayName = "PostHeader";
