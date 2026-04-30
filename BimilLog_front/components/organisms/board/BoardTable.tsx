"use client";

import React, { memo, useMemo } from "react";
import { Card } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import {
  ThumbsUp,
  Eye,
  User,
  Mail,
  PenLine,
} from "lucide-react";
import { FeaturedBadges } from "@/components/organisms/board/featured-badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Popover
} from "flowbite-react";
import { UserActionPopover } from "@/components/molecules/UserActionPopover";
import { BoardTableSkeleton } from "./BoardTableSkeleton";

/**
 * 게시글 목록 테이블 컴포넌트
 *
 * 디자인 결정: 좋아요 기능은 읽기 전용 표시만 제공
 * - 목록 화면: likeCount 정보 표시 (Popover로 상세 정보)
 * - 실제 좋아요 액션: 상세 페이지(PostDetailClient.tsx)에서만 가능
 *
 * 이유: 목록 화면에서의 상호작용 최소화 및 UX 단순화
 */
interface BoardTableProps {
  posts: SimplePost[];
  variant: "all" | "popular" | "legend";

  // 로딩/에러 상태
  isLoading?: boolean;
  error?: Error | null;
  isSearching?: boolean;
  /** 검색 모드일 때 빈 결과 카피에 echo 할 키워드 (round-6) */
  searchTerm?: string;

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
    <TableRow className="bg-card transition-colors hover:bg-accent">
      {/* 순위 또는 번호 */}
      <TableCell className="text-center font-medium text-foreground">
        {showRanking ? (
          <span className="font-display text-lg font-bold text-stamp-red">
            {index + 1}
          </span>
        ) : (
          <span>{post.id}</span>
        )}
      </TableCell>

      {/* 제목 */}
      <TableCell>
        <div className="flex items-center gap-2">
          <FeaturedBadges weekly={post.weekly} legend={post.legend} notice={post.notice} />
          <Link
            href={`/board/post/${post.id}`}
            className={`block line-clamp-2 font-semibold transition-colors ${
              isRead
                ? 'text-muted-foreground'
                : 'text-foreground hover:text-postal-navy dark:hover:text-stamp-red'
            }`}
          >
            {post.title}
            {post.commentCount > 0 && (
              <span className="ml-2 text-stamp-red font-normal">
                [{post.commentCount}]
              </span>
            )}
          </Link>
        </div>
      </TableCell>

      {/* 작성자 */}
      <TableCell className="text-foreground">
        {post.memberName && post.memberName !== "익명" ? (
          <UserActionPopover
            memberName={post.memberName}
            memberId={post.memberId}
            trigger={
              <button className="inline-flex max-w-20 items-center space-x-1 truncate transition-colors hover:text-postal-navy hover:underline dark:hover:text-stamp-red">
                <User className="w-3 h-3" />
                <span>{post.memberName}</span>
              </button>
            }
            placement="bottom"
          />
        ) : (
          <span className="inline-flex max-w-20 items-center space-x-1 truncate text-muted-foreground">
            <User className="w-3 h-3" />
            <span>{post.memberName || "익명"}</span>
          </span>
        )}
      </TableCell>

      {/* 작성일 */}
      <TableCell className="hidden text-sm text-muted-foreground sm:table-cell">
        {formatDate(post.createdAt)}
      </TableCell>

      {/* 추천 */}
      <TableCell className="text-center text-muted-foreground">
        {enablePopover ? (
          <Popover
            trigger="hover"
            placement="top"
            content={
              <div className="p-3 min-w-[180px]">
                <div className="mb-2 flex items-center gap-2">
                  <ThumbsUp className="w-4 h-4 text-stamp-red dark:text-stamp-red" />
                  <span className="text-sm font-semibold text-foreground">좋아요 통계</span>
                </div>
                <div className="space-y-1 text-xs text-muted-foreground">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">현재 좋아요:</span>
                    <span className="font-medium text-foreground">{post.likeCount}개</span>
                  </div>
                  {showRanking && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">순위:</span>
                      <span className="font-medium text-stamp-red dark:text-stamp-red">#{index + 1}</span>
                    </div>
                  )}
                </div>
              </div>
            }
          >
            <div className="flex items-center justify-center gap-1 px-2 py-1 text-xs text-muted-foreground">
              <ThumbsUp className="w-3 h-3" />
              {post.likeCount}
            </div>
          </Popover>
        ) : (
          <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
            <ThumbsUp className="w-3 h-3" />
            {post.likeCount}
          </div>
        )}
      </TableCell>

      {/* 조회수 */}
      <TableCell className="text-center text-muted-foreground">
        {post.viewCount}
      </TableCell>
    </TableRow>
  );
});

// 모바일용 카드 컴포넌트 (A-4: 카드 전체가 Link로 어포던스 강화)
const BoardMobileCard = memo<TableRowProps>(({
  post,
  index,
  isRead,
  showRanking
}) => {
  // popover 트리거 클릭이 부모 카드 링크 라우팅을 막도록 stopPropagation
  const stopPropagation = (e: React.MouseEvent | React.KeyboardEvent) => {
    e.stopPropagation();
  };

  return (
    <Card
      data-testid="board-mobile-card"
      variant="elevated"
      className="relative transition-all hover:shadow-brand-md overflow-visible isolation-auto"
    >
      {/* 카드 전체를 덮는 invisible Link (좌상단 ~ 우하단) */}
      <Link
        href={`/board/post/${post.id}`}
        aria-label={post.title}
        className="absolute inset-0 z-0 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-secondary"
      />
      <div className="relative z-10 p-3 pointer-events-none">
        <div className="mb-1.5 flex items-start justify-between">
          <div className="flex-1">
            {/* 순위 표시 (인기글 탭에서만) */}
            <div className="flex items-center gap-2 mb-1.5">
              {showRanking && (
                <span className="font-display text-lg font-bold text-stamp-red dark:text-stamp-red">
                  #{index + 1}
                </span>
              )}
              <FeaturedBadges weekly={post.weekly} legend={post.legend} notice={post.notice} />
            </div>

            {/* 제목 (overlay link로 클릭 처리되므로 시각적 텍스트만) */}
            <h3
              className={`block text-sm font-semibold transition-colors line-clamp-2 ${
                isRead
                  ? 'text-muted-foreground'
                  : 'text-foreground'
              }`}
            >
              {post.title}
              {post.commentCount > 0 && (
                <span className="ml-2 text-stamp-red font-normal">
                  [{post.commentCount}]
                </span>
              )}
            </h3>
          </div>
        </div>

        {/* 하단 정보 */}
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center gap-2 min-w-0">
            {post.memberName && post.memberName !== "익명" ? (
              <span
                className="pointer-events-auto"
                onClick={stopPropagation}
                onKeyDown={stopPropagation}
                role="presentation"
              >
                <UserActionPopover
                  memberName={post.memberName}
                  memberId={post.memberId}
                  trigger={
                    <button
                      className="inline-flex max-w-32 cursor-pointer items-center gap-1 truncate transition-colors hover:text-postal-navy hover:underline dark:text-gray-200 dark:hover:text-stamp-red"
                      aria-label={`작성자 ${post.memberName}`}
                    >
                      <User className="w-3 h-3 flex-shrink-0" />
                      <span className="truncate">작성자 {post.memberName}</span>
                    </button>
                  }
                  placement="bottom"
                />
              </span>
            ) : (
              <span className="inline-flex max-w-32 items-center gap-1 truncate text-muted-foreground">
                <User className="w-3 h-3 flex-shrink-0" />
                <span className="truncate">{post.memberName || "익명"}</span>
              </span>
            )}
            <span className="text-xs flex-shrink-0">{formatDate(post.createdAt)}</span>
          </div>
          <div className="flex items-center gap-2 flex-shrink-0">
            <span className="flex items-center gap-1 text-xs">
              <ThumbsUp className="w-3 h-3" />
              {post.likeCount}
            </span>
            <span className="flex items-center gap-1 text-xs">
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
  searchTerm = "",
  showRanking = variant !== "all",
  enablePopover = variant !== "all"
}) => {
  // 읽음 상태 추적 - 모든 variant에서 사용
  const postIds = useMemo(() => posts.map(post => post.id), [posts]);
  const { readStatus } = usePostReadStatus(postIds);

  // 에러 상태 처리 — 토큰화 + 종이/편지 메타포 (F-210)
  if (error) {
    return (
      <Card variant="elevated">
        <div className="p-8 text-center text-destructive break-keep">
          편지를 가져오지 못했어요. 잠시 후 다시 시도해 주세요.
          <br />
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-4 text-sm text-postal-navy hover:underline dark:text-stamp-red"
          >
            새로고침
          </button>
        </div>
      </Card>
    );
  }

  // 로딩 상태 처리 - 이전 데이터가 없을 때만 스켈레톤 표시
  if (isLoading && posts.length === 0) {
    return <BoardTableSkeleton rows={6} showRanking={showRanking} />;
  }

  // 빈 상태일 때 한 번만 노출 (B-M4: data-testid 중복 방지)
  // 빈 상태 3종 세트: 일러스트 + 카피 + primary CTA
  if (posts.length === 0) {
    return (
      <Card variant="elevated" className="bg-paper-card border border-ink-soft">
        <div className="p-10 md:p-14 text-center text-ink-soft dark:text-muted-foreground break-keep">
          {isSearching ? (
            <div
              data-testid="board-empty-state"
              className="flex flex-col items-center gap-3"
            >
              <div className="relative w-20 h-20 mb-2">
                <svg viewBox="0 0 80 80" className="w-full h-full" aria-hidden="true">
                  <rect x="10" y="20" width="60" height="44" rx="4" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2" />
                  <line x1="20" y1="32" x2="60" y2="32" stroke="#A89084" strokeWidth="1.5" />
                  <line x1="20" y1="40" x2="55" y2="40" stroke="#A89084" strokeWidth="1.5" />
                  <line x1="20" y1="48" x2="48" y2="48" stroke="#A89084" strokeWidth="1.5" />
                  <circle cx="58" cy="62" r="10" fill="none" stroke="#C73E3E" strokeWidth="2.5" />
                  <line x1="65" y1="69" x2="74" y2="78" stroke="#C73E3E" strokeWidth="2.5" strokeLinecap="round" />
                </svg>
              </div>
              <span className="font-display text-xl font-semibold text-ink dark:text-foreground break-keep">
                {searchTerm
                  ? `‘${searchTerm}’에 해당하는 게시글을 찾지 못했어요`
                  : "검색 결과가 없어요"}
              </span>
              <span className="text-sm max-w-xs break-keep">
                철자를 확인하거나 더 짧은 키워드로 시도해 보세요. 새로운 이야기를 직접 남겨도 좋아요.
              </span>
              <Link
                href="/board/write"
                className="mt-4 inline-flex items-center justify-center min-h-touch px-5 py-2.5 rounded-md bg-paper-button text-white font-semibold shadow-brand-sm hover:bg-paper-hover transition-colors"
              >
                <PenLine className="w-4 h-4 mr-2" /> 첫 글의 주인공 되기
              </Link>
            </div>
          ) : variant === "all" ? (
            <div
              data-testid="board-empty-state"
              className="flex flex-col items-center gap-3"
            >
              <div className="relative w-20 h-20 mb-2">
                <svg viewBox="0 0 80 80" className="w-full h-full">
                  <rect x="14" y="22" width="52" height="40" rx="3" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2" />
                  <polyline points="14,22 40,46 66,22" fill="none" stroke="#2A1F1A" strokeWidth="2" />
                  <circle cx="56" cy="34" r="6" fill="#C73E3E" />
                </svg>
              </div>
              <span className="font-display text-xl font-semibold text-ink dark:text-foreground">
                아직 도착한 편지가 없어요
              </span>
              <span className="text-sm">첫 글의 주인공이 되어보세요</span>
              <Link
                href="/board/write"
                className="mt-4 inline-flex items-center justify-center min-h-touch px-5 py-2.5 rounded-md bg-paper-button text-white font-semibold shadow-brand-sm hover:bg-paper-hover transition-colors"
              >
                <PenLine className="w-4 h-4 mr-2" /> 글쓰기
              </Link>
            </div>
          ) : variant === "legend" ? (
            <div className="flex flex-col items-center gap-3">
              <Mail className="w-10 h-10 text-stamp-red" />
              <span className="font-display text-lg font-semibold text-ink dark:text-foreground">
                아직 등록된 레전드 글이 없어요
              </span>
              <span className="text-sm">역대 최고 인기글이 선정되면 여기에 표시됩니다</span>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-3">
              <Mail className="w-10 h-10 text-stamp-red" />
              <span className="font-display text-lg font-semibold text-ink dark:text-foreground">
                아직 인기글이 없어요
              </span>
              <span className="text-sm">게시글을 작성하고 좋아요를 받아보세요!</span>
              <Link
                href="/board/write"
                className="mt-2 inline-flex items-center justify-center min-h-touch px-5 py-2.5 rounded-md bg-paper-button text-white font-semibold shadow-brand-sm hover:bg-paper-hover transition-colors"
              >
                <PenLine className="w-4 h-4 mr-2" /> 글쓰기
              </Link>
            </div>
          )}
        </div>
      </Card>
    );
  }

  return (
    <>
      {/* 데스크톱 테이블 */}
      <div className="hidden overflow-x-auto sm:block">
        <Table hoverable className="min-w-full text-brand-primary dark:text-gray-100">
          <TableHead className="bg-gray-50 text-brand-secondary dark:bg-slate-900/80 dark:text-gray-300">
            <TableRow>
              <TableHeadCell className="w-20 text-center">
                {showRanking ? "순위" : "번호"}
              </TableHeadCell>
              <TableHeadCell className="text-left">제목</TableHeadCell>
              <TableHeadCell className="w-24 text-left">작성자</TableHeadCell>
              <TableHeadCell className="hidden w-28 text-left sm:table-cell">작성일</TableHeadCell>
              <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
              <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y divide-gray-100 dark:divide-slate-800">
            {posts.map((post, index) => (
              <BoardTableRow
                key={post.id}
                post={post}
                index={index}
                variant={variant}
                isRead={readStatus[post.id] || false}
                showRanking={showRanking}
                enablePopover={enablePopover}
              />
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 */}
      <div className="space-y-3 sm:hidden overflow-visible">
        {posts.map((post, index) => (
          <div key={post.id} className="relative isolation-auto z-card-stack">
            <BoardMobileCard
              post={post}
              index={index}
              variant={variant}
              isRead={readStatus[post.id] || false}
              showRanking={showRanking}
              enablePopover={enablePopover}
            />
          </div>
        ))}
      </div>
    </>
  );
});

BoardTable.displayName = "BoardTable";
