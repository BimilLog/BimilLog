"use client";

import React from "react";
import { Card } from "@/components";
import { Badge } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import { CheckCircle, Megaphone, TrendingUp, Calendar, Crown } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow } from "flowbite-react";

interface PostListProps {
  posts: SimplePost[];
}

interface PostListItemProps {
  post: SimplePost;
}

// 데스크톱용 테이블 행 컴포넌트
const PostListTableItem = React.memo<PostListItemProps & { isRead: boolean; progress: number }>(
  ({ post, isRead, progress }) => (
    <TableRow className="bg-white hover:bg-gray-50">
      <TableCell>
        {post.postCacheFlag === "NOTICE" && (
          <Badge variant="info" icon={Megaphone}>공지</Badge>
        )}
        {post.postCacheFlag === "REALTIME" && (
          <Badge variant="destructive" icon={TrendingUp}>실시간</Badge>
        )}
        {post.postCacheFlag === "WEEKLY" && (
          <Badge variant="warning" icon={Calendar}>주간</Badge>
        )}
        {post.postCacheFlag === "LEGEND" && (
          <Badge variant="purple" icon={Crown}>레전드</Badge>
        )}
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2">
          <Link
            href={`/board/post/${post.id}`}
            className={`font-semibold transition-colors line-clamp-2 block flex-1 ${
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
          {isRead && (
            <span title="읽음">
              <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0" />
            </span>
          )}
          {!isRead && progress > 0 && progress < 90 && (
            <div className="w-12 h-1.5 bg-gray-200 rounded-full flex-shrink-0">
              <div
                className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                style={{ width: `${progress}%` }}
                title={`${Math.round(progress)}% 읽음`}
              />
            </div>
          )}
        </div>
      </TableCell>
      <TableCell>
        <Link
          href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
          className="hover:text-purple-600 hover:underline transition-colors truncate block max-w-20"
          title={`${post.userName}님의 롤링페이퍼 보기`}
        >
          {post.userName}
        </Link>
      </TableCell>
      <TableCell className="text-sm">
        {formatDate(post.createdAt)}
      </TableCell>
      <TableCell className="text-center">{post.viewCount}</TableCell>
      <TableCell className="text-center">{post.likeCount}</TableCell>
    </TableRow>
));

// 모바일용 카드 컴포넌트
const PostListMobileItem = React.memo<PostListItemProps & { isRead: boolean; progress: number }>(
  ({ post, isRead, progress }) => (
    <Card variant="elevated" className="hover:shadow-brand-md transition-all">
      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <div className="flex-1">
            {post.postCacheFlag === "NOTICE" && (
              <Badge variant="info" icon={Megaphone} className="mb-2">공지</Badge>
            )}
            {post.postCacheFlag === "REALTIME" && (
              <Badge variant="destructive" icon={TrendingUp} className="mb-2">실시간</Badge>
            )}
            {post.postCacheFlag === "WEEKLY" && (
              <Badge variant="warning" icon={Calendar} className="mb-2">주간</Badge>
            )}
            {post.postCacheFlag === "LEGEND" && (
              <Badge variant="purple" icon={Crown} className="mb-2">레전드</Badge>
            )}
            <div className="flex items-start gap-2">
              <Link
                href={`/board/post/${post.id}`}
                className={`font-semibold transition-colors line-clamp-2 block text-base flex-1 ${
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
              {isRead && (
                <span title="읽음">
                  <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0 mt-1" />
                </span>
              )}
            </div>
            {!isRead && progress > 0 && progress < 90 && (
              <div className="w-full h-1.5 bg-gray-200 rounded-full mt-2">
                <div
                  className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}
          </div>
        </div>

      <div className="flex items-center justify-between text-sm text-brand-secondary">
        <div className="flex items-center gap-3">
          <Link
            href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
            className="hover:text-purple-600 transition-colors truncate max-w-20"
            title={`${post.userName}님의 롤링페이퍼 보기`}
          >
            {post.userName}
          </Link>
          <span>{formatDate(post.createdAt)}</span>
        </div>
        <div className="flex items-center gap-3">
          <span>조회 {post.viewCount}</span>
          <span>추천 {post.likeCount}</span>
        </div>
      </div>
    </div>
  </Card>
));

PostListTableItem.displayName = "PostListTableItem";
PostListMobileItem.displayName = "PostListMobileItem";

export const PostList = React.memo<PostListProps>(({ posts }) => {
  const regularPosts = posts.filter((post) => !post.isNotice);

  // 읽음 상태 추적
  const postIds = regularPosts.map(post => post.id);
  const { readStatus, progressStatus } = usePostReadStatus(postIds);

  return (
    <>
      {/* 태블릿 이상에서 테이블 형태로 표시 */}
      <div className="hidden sm:block overflow-x-auto">
        <Table hoverable>
          <TableHead>
            <TableRow>
              <TableHeadCell className="w-20">상태</TableHeadCell>
              <TableHeadCell>제목</TableHeadCell>
              <TableHeadCell className="w-24">작성자</TableHeadCell>
              <TableHeadCell className="w-28">작성일</TableHeadCell>
              <TableHeadCell className="text-center w-16">조회</TableHeadCell>
              <TableHeadCell className="text-center w-16">추천</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {regularPosts.length > 0 ? (
              regularPosts.map((post) => (
                <PostListTableItem
                  key={post.id}
                  post={post}
                  isRead={readStatus[post.id] || false}
                  progress={progressStatus[post.id] || 0}
                />
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-12 text-gray-500">
                  게시글이 없습니다.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 모바일에서 카드 형태로 표시 */}
      <div className="sm:hidden space-y-3">
        {regularPosts.length > 0 ? (
          regularPosts.map((post) => (
            <PostListMobileItem
              key={post.id}
              post={post}
              isRead={readStatus[post.id] || false}
              progress={progressStatus[post.id] || 0}
            />
          ))
        ) : (
          <Card variant="elevated">
            <div className="p-8 text-center text-brand-secondary">
              게시글이 없습니다.
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

PostList.displayName = "PostList";
