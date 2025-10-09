"use client";

import React, { memo } from "react";
import { Card } from "@/components";
import { Button } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import {
  ThumbsUp,
  Eye,
  User,
  ExternalLink
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Popover
} from "flowbite-react";

interface BoardTableProps {
  posts: SimplePost[];
  variant: "all" | "popular" | "legend";

  // 로딩/에러 상태
  isLoading?: boolean;
  error?: Error | null;
  isSearching?: boolean;

  // 페이징
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;

  // 옵션
  showRanking?: boolean;
  enablePopover?: boolean;
}

interface TableRowProps {
  post: SimplePost;
  index: number;
  variant: "all" | "popular" | "legend";
  isRead: boolean;
  showRanking?: boolean;
  enablePopover?: boolean;
}

// 데스크톱용 테이블 행 컴포넌트
const BoardTableRow = memo<TableRowProps>(({
  post,
  index,
  isRead,
  showRanking,
  enablePopover
}) => {

  return (
    <TableRow className="bg-white hover:bg-gray-50">
      {/* 순위 또는 번호 */}
      <TableCell className="text-center font-medium">
        {showRanking ? (
          <span className="text-lg font-bold text-purple-600">
            {index + 1}
          </span>
        ) : (
          <span>{post.id}</span>
        )}
      </TableCell>

      {/* 제목 */}
      <TableCell>
        <Link
          href={`/board/post/${post.id}`}
          className={`font-semibold transition-colors line-clamp-2 block ${
            isRead ? 'text-gray-500' : 'text-gray-900 hover:text-purple-600'
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

      {/* 작성자 */}
      <TableCell>
        {post.memberName && post.memberName !== "익명" ? (
          <Popover
            trigger="click"
            placement="bottom"
            content={
              <div className="p-3 w-56">
                <div className="flex flex-col space-y-2">
                  <div className="flex items-center space-x-2">
                    <User className="w-4 h-4" />
                    <span className="font-medium">{post.memberName}</span>
                  </div>
                  <Link
                    href={`/rolling-paper/${encodeURIComponent(post.memberName)}`}
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
            <button className="truncate max-w-20 hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1">
              <User className="w-3 h-3" />
              <span>{post.memberName}</span>
            </button>
          </Popover>
        ) : (
          <span className="truncate max-w-20 text-gray-500">
            {post.memberName || "익명"}
          </span>
        )}
      </TableCell>

      {/* 작성일 */}
      <TableCell className="text-sm hidden sm:table-cell">
        {formatDate(post.createdAt)}
      </TableCell>

      {/* 추천 */}
      <TableCell className="text-center">
        {enablePopover ? (
          <Popover
            trigger="hover"
            placement="top"
            content={
              <div className="p-3 min-w-[180px]">
                <div className="flex items-center gap-2 mb-2">
                  <ThumbsUp className="w-4 h-4 text-purple-600" />
                  <span className="font-semibold text-sm">좋아요 통계</span>
                </div>
                <div className="space-y-1 text-xs">
                  <div className="flex justify-between">
                    <span className="text-gray-500">현재 좋아요:</span>
                    <span className="font-medium">{post.likeCount}개</span>
                  </div>
                  {showRanking && (
                    <div className="flex justify-between">
                      <span className="text-gray-500">순위:</span>
                      <span className="font-medium text-purple-600">#{index + 1}</span>
                    </div>
                  )}
                </div>
              </div>
            }
          >
            <span className="flex items-center justify-center cursor-help">
              <ThumbsUp className="w-3 h-3 mr-1" />
              {post.likeCount}
            </span>
          </Popover>
        ) : (
          <span>{post.likeCount}</span>
        )}
      </TableCell>

      {/* 조회수 */}
      <TableCell className="text-center">
        {post.viewCount}
      </TableCell>
    </TableRow>
  );
});

// 모바일용 카드 컴포넌트
const BoardMobileCard = memo<TableRowProps>(({
  post,
  index,
  isRead,
  showRanking
}) => {

  return (
    <Card variant="elevated" className="hover:shadow-brand-md transition-all">
      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <div className="flex-1">
            {/* 번호/순위 표시 */}
            <div className="flex items-center gap-2 mb-2">
              {/* 순위 또는 번호 */}
              {showRanking ? (
                <span className="text-xl font-bold text-purple-600">
                  #{index + 1}
                </span>
              ) : (
                <span className="text-xs text-gray-600">
                  번호: {post.id}
                </span>
              )}
            </div>

            {/* 제목 */}
            <Link
              href={`/board/post/${post.id}`}
              className={`font-semibold transition-colors line-clamp-2 block text-base ${
                isRead ? 'text-gray-500' : 'text-brand-primary hover:text-purple-600'
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
        <div className="flex items-center justify-between text-sm text-brand-secondary">
          <div className="flex items-center gap-3">
            {post.memberName && post.memberName !== "익명" ? (
              <Popover
                trigger="click"
                placement="bottom"
                content={
                  <div className="p-3 w-56">
                    <div className="flex flex-col space-y-2">
                      <div className="flex items-center space-x-2">
                        <User className="w-4 h-4" />
                        <span className="font-medium">{post.memberName}</span>
                      </div>
                      <Link
                        href={`/rolling-paper/${encodeURIComponent(post.memberName)}`}
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
                <button className="hover:text-purple-600 transition-colors truncate max-w-20 cursor-pointer inline-flex items-center space-x-1">
                  <User className="w-3 h-3" />
                  <span>{post.memberName}</span>
                </button>
              </Popover>
            ) : (
              <span className="truncate max-w-20 text-gray-500">
                {post.memberName || "익명"}
              </span>
            )}
            <span>{formatDate(post.createdAt)}</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <ThumbsUp className="w-3 h-3" />
              {post.likeCount}
            </span>
            <span className="flex items-center gap-1">
              <Eye className="w-3 h-3" />
              {post.viewCount}
            </span>
          </div>
        </div>
      </div>
    </Card>
  );
});

BoardTableRow.displayName = "BoardTableRow";
BoardMobileCard.displayName = "BoardMobileCard";

export const BoardTable = memo<BoardTableProps>(({
  posts,
  variant,
  isLoading = false,
  error = null,
  isSearching = false,
  showRanking = variant !== "all",
  enablePopover = variant !== "all"
}) => {
  // 읽음 상태 추적 - 모든 variant에서 사용
  const postIds = posts.map(post => post.id);
  const { readStatus } = usePostReadStatus(postIds);

  // 에러 상태 처리
  if (error) {
    return (
      <Card variant="elevated">
        <div className="p-8 text-center text-red-500">
          게시글을 불러오는 중 오류가 발생했습니다.
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
  if (isLoading) {
    return (
      <>
        {/* 데스크톱 로딩 스켈레톤 */}
        <div className="hidden sm:block overflow-x-auto">
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell className="w-20 text-center">
                  {showRanking ? "순위" : "번호"}
                </TableHeadCell>
                <TableHeadCell>제목</TableHeadCell>
                <TableHeadCell className="w-24">작성자</TableHeadCell>
                <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
                <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
                <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
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
                  <TableCell>
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-16" />
                  </TableCell>
                  <TableCell className="hidden sm:table-cell">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-20" />
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-8 mx-auto" />
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-8 mx-auto" />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {/* 모바일 로딩 스켈레톤 */}
        <div className="sm:hidden space-y-3">
          {[...Array(5)].map((_, idx) => (
            <Card key={idx} variant="elevated">
              <div className="p-4 space-y-3">
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
      <div className="hidden sm:block overflow-x-auto">
        <Table hoverable>
          <TableHead>
            <TableRow>
              <TableHeadCell className="w-20 text-center">
                {showRanking ? "순위" : "번호"}
              </TableHeadCell>
              <TableHeadCell>제목</TableHeadCell>
              <TableHeadCell className="w-24">작성자</TableHeadCell>
              <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
              <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
              <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {posts.length > 0 ? (
              posts.map((post, index) => (
                <BoardTableRow
                  key={post.id}
                  post={post}
                  index={index}
                  variant={variant}
                  isRead={readStatus[post.id] || false}
                  showRanking={showRanking}
                  enablePopover={enablePopover}
                />
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center py-12"
                >
                  {isSearching ? (
                    <span className="text-gray-500">검색 결과가 없습니다.</span>
                  ) : variant === "all" ? (
                    <span className="text-gray-500">게시글이 없습니다.</span>
                  ) : (
                    <div className="flex flex-col items-center gap-2">
                      <span className="text-gray-700 font-medium">아직 인기글이 없어요 😊</span>
                      <span className="text-sm text-gray-500">게시글을 작성하고 좋아요를 받아보세요!</span>
                    </div>
                  )}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 */}
      <div className="sm:hidden space-y-3">
        {posts.length > 0 ? (
          posts.map((post, index) => (
            <BoardMobileCard
              key={post.id}
              post={post}
              index={index}
              variant={variant}
              isRead={readStatus[post.id] || false}
              showRanking={showRanking}
              enablePopover={enablePopover}
            />
          ))
        ) : (
          <Card variant="elevated">
            <div className="p-8 text-center">
              {isSearching ? (
                <span className="text-brand-secondary">검색 결과가 없습니다.</span>
              ) : variant === "all" ? (
                <span className="text-brand-secondary">게시글이 없습니다.</span>
              ) : (
                <div className="flex flex-col items-center gap-2">
                  <span className="text-gray-700 font-medium">아직 인기글이 없어요 😊</span>
                  <span className="text-sm text-gray-500">게시글을 작성하고 좋아요를 받아보세요!</span>
                </div>
              )}
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

BoardTable.displayName = "BoardTable";