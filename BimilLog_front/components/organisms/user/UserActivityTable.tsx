"use client";

import React, { memo } from "react";
import { Card, SafeHTML } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/types/domains/post";
import { type SimpleComment } from "@/types/domains/comment";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import {
  ThumbsUp,
  Eye,
  MessageCircle,
  ExternalLink,
  Mail,
  Reply,
  Heart,
  MailOpen,
  RefreshCw
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow
} from "flowbite-react";
import { UserActivityTableSkeleton } from "./UserActivityTableSkeleton";

type TabType = "my-posts" | "my-comments" | "liked-posts" | "liked-comments";

interface UserActivityTableProps {
  items: (SimplePost | SimpleComment)[];
  contentType: "posts" | "comments";
  tabType: TabType;
  isLoading?: boolean;
  error?: Error | null;
  /**
   * 에러 발생 시 호출되는 재시도 핸들러. 미제공 시 fallback 으로
   * window.location.reload 가 호출되지만, B-310 회귀 방지를 위해
   * UserActivitySection 에서 refetch 를 prop 으로 내려주는 것을 권장.
   */
  onRetry?: () => void;
}

// 종이/편지 메타포 빈 상태 카피 — 라운드 9 일관
const EMPTY_STATE: Record<TabType, { icon: React.ComponentType<{ className?: string; "aria-hidden"?: boolean }>; copy: string }> = {
  "my-posts": {
    icon: Mail,
    copy: "아직 작성한 편지가 없어요. 게시판에 첫 편지를 띄워볼까요?",
  },
  "my-comments": {
    icon: Reply,
    copy: "아직 남긴 답장이 없어요. 누군가의 글에 첫 응답을 남겨보세요.",
  },
  "liked-posts": {
    icon: Heart,
    copy: "아직 좋아요한 편지가 없어요. 마음에 드는 글에 ♡ 를 눌러주세요.",
  },
  "liked-comments": {
    icon: MailOpen,
    copy: "아직 좋아요한 답장이 없어요. 공감 가는 댓글에 ♡ 를 눌러주세요.",
  },
};

interface TableRowProps {
  item: SimplePost | SimpleComment;
  contentType: "posts" | "comments";
  isRead?: boolean;
}

// 타입 가드 함수
function isPost(item: SimplePost | SimpleComment): item is SimplePost {
  return 'viewCount' in item && 'commentCount' in item;
}

// 데스크톱용 테이블 행 컴포넌트
const UserActivityTableRow = memo<TableRowProps>(({
  item,
  contentType,
  isRead = false
}) => {
  if (contentType === "posts" && isPost(item)) {
    const post = item;
    return (
      <TableRow className="bg-card transition-colors hover:bg-accent">
        {/* 번호 */}
        <TableCell className="w-20 text-center font-medium text-foreground">
          {post.id}
        </TableCell>

        {/* 제목 */}
        <TableCell>
          <Link
            href={`/board/post/${post.id}`}
            className={`block line-clamp-2 font-semibold transition-colors ${
              isRead
                ? 'text-muted-foreground'
                : 'text-foreground hover:text-purple-600 dark:hover:text-purple-300'
            }`}
          >
            {post.title}
            {post.commentCount > 0 && (
              <span className="ml-2 text-purple-500 font-normal">
                [{post.commentCount}]
              </span>
            )}
          </Link>
        </TableCell>

        {/* 작성일 */}
        <TableCell className="hidden w-28 text-sm text-muted-foreground sm:table-cell">
          {formatDate(post.createdAt)}
        </TableCell>

        {/* 추천 */}
        <TableCell className="w-20 text-center text-muted-foreground">
          <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
            <ThumbsUp className="w-3 h-3" />
            {post.likeCount}
          </div>
        </TableCell>

        {/* 조회수 */}
        <TableCell className="w-20 text-center text-muted-foreground">
          <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
            <Eye className="w-3 h-3" />
            {post.viewCount}
          </div>
        </TableCell>

        {/* 댓글수 */}
        <TableCell className="w-20 text-center text-muted-foreground">
          <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
            <MessageCircle className="w-3 h-3" />
            {post.commentCount}
          </div>
        </TableCell>
      </TableRow>
    );
  }

  // 댓글인 경우
  const comment = item as SimpleComment;
  return (
    <TableRow className="bg-card transition-colors hover:bg-accent">
      {/* 번호 */}
      <TableCell className="w-20 text-center font-medium text-foreground">
        {comment.id}
      </TableCell>

      {/* 댓글 내용 */}
      <TableCell>
        <SafeHTML html={comment.content} className="line-clamp-2 text-foreground" />
      </TableCell>

      {/* 작성일 */}
      <TableCell className="hidden w-28 text-sm text-muted-foreground sm:table-cell">
        {formatDate(comment.createdAt)}
      </TableCell>

      {/* 추천 */}
      <TableCell className="w-20 text-center text-muted-foreground">
        <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
          <ThumbsUp className="w-3 h-3" />
          {comment.likeCount}
        </div>
      </TableCell>

      {/* 게시글 보기 */}
      <TableCell className="w-24 text-center">
        <Link
          href={`/board/post/${comment.postId}#comment-${comment.id}`}
          className="font-medium text-purple-600 hover:underline dark:text-purple-500 flex items-center justify-center space-x-1"
        >
          <span className="hidden sm:inline">보기</span>
          <ExternalLink className="w-4 h-4" />
        </Link>
      </TableCell>
    </TableRow>
  );
});

// 모바일용 카드 컴포넌트
const UserActivityMobileCard = memo<TableRowProps>(({
  item,
  contentType,
  isRead = false
}) => {
  if (contentType === "posts" && isPost(item)) {
    const post = item;
    return (
      <Card variant="elevated" className="transition-all hover:shadow-brand-md">
        <div className="p-3">
          <div className="mb-1.5 flex items-start justify-between">
            <div className="flex-1">
              {/* 제목 */}
              <Link
                href={`/board/post/${post.id}`}
                className={`block text-sm font-semibold transition-colors line-clamp-2 ${
                  isRead
                    ? 'text-muted-foreground'
                    : 'text-foreground hover:text-purple-600 dark:hover:text-purple-300'
                }`}
              >
                {post.title}
                {post.commentCount > 0 && (
                  <span className="ml-2 text-purple-500 font-normal">
                    [{post.commentCount}]
                  </span>
                )}
              </Link>
            </div>
          </div>

          {/* 하단 정보 */}
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>{formatDate(post.createdAt)}</span>
            <div className="flex items-center gap-2">
              <span className="flex items-center gap-1">
                <ThumbsUp className="w-3 h-3" />
                {post.likeCount}
              </span>
              <span className="flex items-center gap-1">
                <Eye className="w-3 h-3" />
                {post.viewCount}
              </span>
              <span className="flex items-center gap-1">
                <MessageCircle className="w-3 h-3" />
                {post.commentCount}
              </span>
            </div>
          </div>
        </div>
      </Card>
    );
  }

  // 댓글인 경우
  const comment = item as SimpleComment;
  return (
    <Card variant="elevated" className="transition-all hover:shadow-brand-md">
      <div className="p-3">
        <div className="mb-1.5">
          <SafeHTML html={comment.content} className="text-sm line-clamp-2 text-foreground" />
        </div>

        {/* 하단 정보 */}
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>{formatDate(comment.createdAt)}</span>
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1">
              <ThumbsUp className="w-3 h-3" />
              {comment.likeCount}
            </span>
            <Link
              href={`/board/post/${comment.postId}#comment-${comment.id}`}
              className="flex items-center gap-1 text-purple-600 hover:underline dark:text-purple-500"
            >
              <ExternalLink className="w-3 h-3" />
            </Link>
          </div>
        </div>
      </div>
    </Card>
  );
});

UserActivityTableRow.displayName = "UserActivityTableRow";
UserActivityMobileCard.displayName = "UserActivityMobileCard";

export const UserActivityTable = memo<UserActivityTableProps>(({
  items,
  contentType,
  tabType,
  isLoading = false,
  error = null,
  onRetry,
}) => {
  // 읽음 상태 추적 - 게시글만
  const postIds = contentType === "posts" ? items.map(item => item.id) : [];
  const { readStatus } = usePostReadStatus(postIds);
  const effectiveReadStatus: Record<number, boolean> = contentType === "posts" ? readStatus : {};

  // 에러 상태 처리 — B-310: refetch 가 가능하면 SPA 컨텍스트 보존,
  // 아니면 마지막 fallback 으로만 page reload.
  if (error) {
    return (
      <Card variant="elevated">
        <div className="p-8 text-center">
          <p className="text-stamp-red font-medium break-keep">
            편지를 불러오지 못했어요.
          </p>
          <p className="text-sm text-ink-soft dark:text-ink-300 mt-2 break-keep">
            잠시 후 다시 시도해주세요.
          </p>
          <button
            type="button"
            onClick={() => {
              if (onRetry) {
                onRetry();
              } else {
                window.location.reload();
              }
            }}
            className="mt-4 inline-flex items-center gap-1.5 text-sm text-postal-navy hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1 rounded"
          >
            <RefreshCw className="w-4 h-4" aria-hidden="true" />
            다시 시도
          </button>
        </div>
      </Card>
    );
  }

  // 로딩 상태 처리
  if (isLoading && items.length === 0) {
    return <UserActivityTableSkeleton contentType={contentType} />;
  }

  return (
    <>
      {/* 데스크톱 테이블 */}
      <div className="hidden overflow-x-auto sm:block">
        <Table hoverable className="min-w-full text-foreground">
          <TableHead className="bg-muted text-muted-foreground">
            <TableRow>
              <TableHeadCell className="w-20 text-center">번호</TableHeadCell>
              <TableHeadCell className="text-left">
                {contentType === "posts" ? "제목" : "댓글 내용"}
              </TableHeadCell>
              <TableHeadCell className="hidden w-28 text-left sm:table-cell">작성일</TableHeadCell>
              <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
              {contentType === "posts" ? (
                <>
                  <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
                  <TableHeadCell className="w-20 text-center">댓글</TableHeadCell>
                </>
              ) : (
                <TableHeadCell className="w-24 text-center">게시글 보기</TableHeadCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody className="divide-y divide-border">
            {items.length > 0 ? (
              items.map((item) => (
                <UserActivityTableRow
                  key={`${tabType}-${item.id}`}
                  item={item}
                  contentType={contentType}
                  isRead={effectiveReadStatus[item.id] || false}
                />
              ))
            ) : (
              <TableRow className="bg-card">
                <TableCell
                  colSpan={contentType === "posts" ? 6 : 5}
                  className="py-12 text-center"
                >
                  {(() => {
                    const { icon: EmptyIcon, copy } = EMPTY_STATE[tabType];
                    return (
                      <div className="flex flex-col items-center gap-2 text-ink-soft dark:text-ink-300">
                        <EmptyIcon className="w-8 h-8 text-postal-navy/60" aria-hidden />
                        <span className="break-keep">{copy}</span>
                      </div>
                    );
                  })()}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 */}
      <div className="space-y-3 sm:hidden">
        {items.length > 0 ? (
          items.map((item) => (
            <UserActivityMobileCard
              key={`${tabType}-${item.id}`}
              item={item}
              contentType={contentType}
              isRead={effectiveReadStatus[item.id] || false}
            />
          ))
        ) : (
          <Card variant="elevated">
            <div className="p-8 text-center">
              {(() => {
                const { icon: EmptyIcon, copy } = EMPTY_STATE[tabType];
                return (
                  <div className="flex flex-col items-center gap-2 text-ink-soft dark:text-ink-300">
                    <EmptyIcon className="w-8 h-8 text-postal-navy/60" aria-hidden />
                    <span className="break-keep">{copy}</span>
                  </div>
                );
              })()}
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

UserActivityTable.displayName = "UserActivityTable";
