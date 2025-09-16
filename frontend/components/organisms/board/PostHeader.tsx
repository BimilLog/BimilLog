"use client";

import React, { useCallback } from "react";
import { useToast } from "@/hooks";
import { Badge, Button, CardHeader, CardTitle } from "@/components";
import {
  Eye,
  ThumbsUp,
  MessageSquare,
  Lock,
  Edit,
  Trash2,
  User,
  ExternalLink,
  Megaphone,
  TrendingUp,
  Calendar,
  Crown,
  Award,
  Share2,
} from "lucide-react";
import { Post } from "@/lib/api";
import Link from "next/link";
import { KakaoShareButton } from "@/components";
import { Popover, Button as FlowbiteButton } from "flowbite-react";
import { formatDateTime } from "@/lib/utils/date";

interface PostHeaderProps {
  post: Post;
  commentCount: number;
  canModify: () => boolean;
  onDeleteClick: () => void;
}

export const PostHeader = React.memo<PostHeaderProps>(({
  post,
  commentCount,
  canModify,
  onDeleteClick,
}) => {
  const { showSuccess, showError } = useToast();

  // 링크 공유 기능 (롤링페이퍼와 동일한 로직)
  const handleWebShare = useCallback(async () => {
    const shareData = {
      title: `${post.title} | 비밀로그`,
      text: `${post.userName || "익명"}님이 작성한 글`,
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
  }, [post.id, post.title, post.userName, showSuccess, showError]);

  return (
    <CardHeader className="border-b p-4 md:p-6">
      {/* 제목과 배지 */}
      <div className="mb-4">
        <div className="flex items-center flex-wrap gap-2 mb-3">
          {post.password && <Lock className="w-4 h-4 text-red-500" />}
          {post.isNotice && (
            <Popover
              trigger="hover"
              placement="top"
              content={
                <div className="p-3 max-w-xs">
                  <div className="flex items-center gap-2 mb-2">
                    <Megaphone className="w-4 h-4 text-blue-600" />
                    <span className="font-semibold text-sm">공지사항</span>
                  </div>
                  <p className="text-xs text-gray-600">
                    관리자가 지정한 중요한 공지사항입니다.
                    상단에 고정되어 표시됩니다.
                  </p>
                </div>
              }
            >
              <Badge variant="info" icon={Megaphone}>공지</Badge>
            </Popover>
          )}
          {post.postCacheFlag === "REALTIME" && (
            <Popover
              trigger="hover"
              placement="top"
              content={
                <div className="p-3 max-w-xs">
                  <div className="flex items-center gap-2 mb-2">
                    <TrendingUp className="w-4 h-4 text-red-600" />
                    <span className="font-semibold text-sm">실시간 인기글</span>
                  </div>
                  <p className="text-xs text-gray-600">
                    최근 24시간 동안 조회수와 좋아요가 급상승한 인기 게시글입니다.
                  </p>
                  <div className="mt-2 pt-2 border-t">
                    <div className="flex items-center gap-1 text-xs text-gray-500">
                      <Award className="w-3 h-3" />
                      <span>실시간 업데이트</span>
                    </div>
                  </div>
                </div>
              }
            >
              <Badge variant="destructive" icon={TrendingUp}>실시간</Badge>
            </Popover>
          )}
          {post.postCacheFlag === "WEEKLY" && (
            <Popover
              trigger="hover"
              placement="top"
              content={
                <div className="p-3 max-w-xs">
                  <div className="flex items-center gap-2 mb-2">
                    <Calendar className="w-4 h-4 text-yellow-600" />
                    <span className="font-semibold text-sm">주간 인기글</span>
                  </div>
                  <p className="text-xs text-gray-600">
                    최근 7일간 많은 사랑을 받은 게시글입니다.
                    매주 월요일 자정에 선정됩니다.
                  </p>
                  <div className="mt-2 pt-2 border-t">
                    <div className="flex items-center gap-1 text-xs text-gray-500">
                      <Award className="w-3 h-3" />
                      <span>주간 TOP 게시글</span>
                    </div>
                  </div>
                </div>
              }
            >
              <Badge variant="warning" icon={Calendar}>주간</Badge>
            </Popover>
          )}
          {post.postCacheFlag === "LEGEND" && (
            <Popover
              trigger="hover"
              placement="top"
              content={
                <div className="p-3 max-w-xs">
                  <div className="flex items-center gap-2 mb-2">
                    <Crown className="w-4 h-4 text-purple-600" />
                    <span className="font-semibold text-sm">레전드 게시글</span>
                  </div>
                  <p className="text-xs text-gray-600">
                    역대 최고의 인기를 기록한 전설적인 게시글입니다.
                    좋아요 100개 이상, 댓글 50개 이상 달성 시 선정됩니다.
                  </p>
                  <div className="mt-2 pt-2 border-t">
                    <div className="flex items-center gap-1 text-xs text-gray-500">
                      <Award className="w-3 h-3" />
                      <span>명예의 전당</span>
                    </div>
                  </div>
                </div>
              }
            >
              <Badge variant="purple" icon={Crown}>레전드</Badge>
            </Popover>
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
              {post.userName && post.userName !== "익명" ? (
                <Popover
                  trigger="click"
                  placement="bottom"
                  content={
                    <div className="p-3 w-56">
                      <div className="flex flex-col space-y-2">
                        <div className="flex items-center space-x-2">
                          <User className="w-4 h-4" />
                          <span className="font-medium">{post.userName}</span>
                        </div>
                        <Link
                          href={`/rolling-paper/${encodeURIComponent(
                            post.userName
                          )}`}
                        >
                          <Button size="sm" className="w-full justify-start">
                            <ExternalLink className="w-4 h-4 mr-2" />
                            롤링페이퍼 보기
                          </Button>
                        </Link>
                      </div>
                    </div>
                  }
                >
                  <button className="truncate max-w-[120px] md:max-w-none hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1">
                    <User className="w-3 h-3" />
                    <span>{post.userName}</span>
                  </button>
                </Popover>
              ) : (
                <span className="truncate max-w-[120px] md:max-w-none text-brand-secondary">
                  {post.userName || "익명"}
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
              <Eye className="w-4 h-4" />
              <span>{post.viewCount}</span>
            </div>
            <div className="flex items-center space-x-1">
              <ThumbsUp className="w-4 h-4" />
              <span>{post.likeCount}</span>
            </div>
            <div className="flex items-center space-x-1">
              <MessageSquare className="w-4 h-4" />
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
            author={post.userName || "익명"}
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

          {canModify() && (
            <div className="flex gap-2">
              <Link
                href={`/board/post/${post.id}/edit`}
                className="flex-1 sm:flex-initial"
              >
                <Button
                  size="sm"
                  variant="outline"
                  className="w-full sm:w-auto"
                >
                  <Edit className="w-4 h-4 sm:mr-1" />
                  <span className="hidden sm:inline">수정</span>
                </Button>
              </Link>
              <Button
                size="sm"
                variant="destructive"
                onClick={onDeleteClick}
                className="flex-1 sm:flex-initial"
              >
                <Trash2 className="w-4 h-4 sm:mr-1" />
                <span className="hidden sm:inline">삭제</span>
              </Button>
            </div>
          )}
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
    prevProps.post.isNotice === nextProps.post.isNotice &&
    prevProps.post.postCacheFlag === nextProps.post.postCacheFlag &&
    prevProps.commentCount === nextProps.commentCount
  );
});

PostHeader.displayName = "PostHeader";
