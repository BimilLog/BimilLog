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
  ExternalLink
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow
} from "flowbite-react";

interface UserActivityTableProps {
  items: (SimplePost | SimpleComment)[];
  contentType: "posts" | "comments";
  tabType: "my-posts" | "my-comments" | "liked-posts" | "liked-comments";
  isLoading?: boolean;
  error?: Error | null;
}

interface TableRowProps {
  item: SimplePost | SimpleComment;
  index: number;
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
  index,
  contentType,
  isRead = false
}) => {
  if (contentType === "posts" && isPost(item)) {
    const post = item;
    return (
      <TableRow className="bg-white transition-colors hover:bg-gray-50 dark:bg-slate-900/70 dark:hover:bg-slate-800/80">
        {/* 번호 */}
        <TableCell className="w-20 text-center font-medium text-brand-primary dark:text-gray-100">
          {post.id}
        </TableCell>

        {/* 제목 */}
        <TableCell>
          <Link
            href={`/board/post/${post.id}`}
            className={`block line-clamp-2 font-semibold transition-colors ${
              isRead
                ? 'text-gray-500 dark:text-gray-400'
                : 'text-gray-900 hover:text-purple-600 dark:text-gray-100 dark:hover:text-purple-300'
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
        <TableCell className="hidden w-28 text-sm text-brand-secondary dark:text-gray-300 sm:table-cell">
          {formatDate(post.createdAt)}
        </TableCell>

        {/* 추천 */}
        <TableCell className="w-20 text-center text-brand-secondary dark:text-gray-300">
          <div className="flex items-center justify-center gap-1 text-xs text-gray-600 dark:text-gray-300">
            <ThumbsUp className="w-3 h-3" />
            {post.likeCount}
          </div>
        </TableCell>

        {/* 조회수 */}
        <TableCell className="w-20 text-center text-brand-secondary dark:text-gray-300">
          <div className="flex items-center justify-center gap-1 text-xs text-gray-600 dark:text-gray-300">
            <Eye className="w-3 h-3" />
            {post.viewCount}
          </div>
        </TableCell>

        {/* 댓글수 */}
        <TableCell className="w-20 text-center text-brand-secondary dark:text-gray-300">
          <div className="flex items-center justify-center gap-1 text-xs text-gray-600 dark:text-gray-300">
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
    <TableRow className="bg-white transition-colors hover:bg-gray-50 dark:bg-slate-900/70 dark:hover:bg-slate-800/80">
      {/* 번호 */}
      <TableCell className="w-20 text-center font-medium text-brand-primary dark:text-gray-100">
        {comment.id}
      </TableCell>

      {/* 댓글 내용 */}
      <TableCell>
        <SafeHTML html={comment.content} className="line-clamp-2 text-gray-900 dark:text-gray-100" />
      </TableCell>

      {/* 작성일 */}
      <TableCell className="hidden w-28 text-sm text-brand-secondary dark:text-gray-300 sm:table-cell">
        {formatDate(comment.createdAt)}
      </TableCell>

      {/* 추천 */}
      <TableCell className="w-20 text-center text-brand-secondary dark:text-gray-300">
        <div className="flex items-center justify-center gap-1 text-xs text-gray-600 dark:text-gray-300">
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
  index,
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
                    ? 'text-gray-500 dark:text-gray-400'
                    : 'text-brand-primary hover:text-purple-600 dark:text-gray-100 dark:hover:text-purple-300'
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
          <div className="flex items-center justify-between text-xs text-brand-secondary dark:text-gray-300">
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
          <SafeHTML html={comment.content} className="text-sm line-clamp-2 text-brand-primary dark:text-gray-100" />
        </div>

        {/* 하단 정보 */}
        <div className="flex items-center justify-between text-xs text-brand-secondary dark:text-gray-300">
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
  error = null
}) => {
  // 읽음 상태 추적 - 게시글만
  const postIds = contentType === "posts" ? items.map(item => item.id) : [];
  const { readStatus } = contentType === "posts" ? usePostReadStatus(postIds) : { readStatus: {} };

  // 에러 상태 처리
  if (error) {
    return (
      <Card variant="elevated">
        <div className="p-8 text-center text-red-500">
          데이터를 불러오는 중 오류가 발생했습니다.
          <br />
          <button
            onClick={() => window.location.reload()}
            className="mt-4 text-sm text-blue-600 hover:underline"
          >
            새로고침
          </button>
        </div>
      </Card>
    );
  }

  // 로딩 상태 처리
  if (isLoading && items.length === 0) {
    return (
      <>
        {/* 데스크톱 로딩 스켈레톤 */}
        <div className="hidden sm:block overflow-x-auto">
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell className="w-20 text-center">번호</TableHeadCell>
                <TableHeadCell>{contentType === "posts" ? "제목" : "댓글 내용"}</TableHeadCell>
                <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
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
            <TableBody className="divide-y">
              {[...Array(5)].map((_, idx) => (
                <TableRow key={idx}>
                  <TableCell className="text-center">
                    <div className="h-4 bg-gray-200 rounded animate-pulse" />
                  </TableCell>
                  <TableCell>
                    <div className="h-4 bg-gray-200 rounded animate-pulse" />
                  </TableCell>
                  <TableCell className="hidden sm:table-cell">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-20" />
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-8 mx-auto" />
                  </TableCell>
                  {contentType === "posts" ? (
                    <>
                      <TableCell className="text-center">
                        <div className="h-4 bg-gray-200 rounded animate-pulse w-8 mx-auto" />
                      </TableCell>
                      <TableCell className="text-center">
                        <div className="h-4 bg-gray-200 rounded animate-pulse w-8 mx-auto" />
                      </TableCell>
                    </>
                  ) : (
                    <TableCell className="text-center">
                      <div className="h-4 bg-gray-200 rounded animate-pulse w-16 mx-auto" />
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {/* 모바일 로딩 스켈레톤 */}
        <div className="sm:hidden space-y-3">
          {[...Array(5)].map((_, idx) => (
            <Card key={idx} variant="elevated">
              <div className="p-3 space-y-2">
                <div className="h-4 bg-gray-200 rounded animate-pulse" />
                <div className="h-4 bg-gray-200 rounded animate-pulse w-3/4" />
                <div className="flex justify-between">
                  <div className="h-3 bg-gray-200 rounded animate-pulse w-20" />
                  <div className="h-3 bg-gray-200 rounded animate-pulse w-16" />
                </div>
              </div>
            </Card>
          ))}
        </div>
      </>
    );
  }

  return (
    <>
      {/* 데스크톱 테이블 */}
      <div className="hidden overflow-x-auto sm:block">
        <Table hoverable className="min-w-full text-brand-primary dark:text-gray-100">
          <TableHead className="bg-gray-50 text-brand-secondary dark:bg-slate-900/80 dark:text-gray-300">
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
          <TableBody className="divide-y divide-gray-100 dark:divide-slate-800">
            {items.length > 0 ? (
              items.map((item, index) => (
                <UserActivityTableRow
                  key={`${tabType}-${item.id}`}
                  item={item}
                  index={index}
                  contentType={contentType}
                  isRead={contentType === "posts" ? readStatus[item.id] || false : false}
                />
              ))
            ) : (
              <TableRow className="bg-white dark:bg-slate-900/70">
                <TableCell
                  colSpan={contentType === "posts" ? 6 : 5}
                  className="py-12 text-center"
                >
                  <span className="text-gray-500 dark:text-gray-400">
                    {contentType === "posts" ? "작성한 게시글이 없습니다." : "작성한 댓글이 없습니다."}
                  </span>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 */}
      <div className="space-y-3 sm:hidden">
        {items.length > 0 ? (
          items.map((item, index) => (
            <UserActivityMobileCard
              key={`${tabType}-${item.id}`}
              item={item}
              index={index}
              contentType={contentType}
              isRead={contentType === "posts" ? readStatus[item.id] || false : false}
            />
          ))
        ) : (
          <Card variant="elevated">
            <div className="p-8 text-center text-brand-secondary dark:text-gray-300">
              <span>
                {contentType === "posts" ? "작성한 게시글이 없습니다." : "작성한 댓글이 없습니다."}
              </span>
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

UserActivityTable.displayName = "UserActivityTable";
