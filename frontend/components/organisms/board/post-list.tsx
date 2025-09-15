"use client";

import React from "react";
import { Card, CardContent } from "@/components";
import { Badge } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import { CheckCircle } from "lucide-react";

interface PostListProps {
  posts: SimplePost[];
}

interface PostListItemProps {
  post: SimplePost;
}

// 데스크톱용 테이블 행 컴포넌트
const PostListTableItem = React.memo<PostListItemProps & { isRead: boolean; progress: number }>(
  ({ post, isRead, progress }) => (
    <tr className="border-b border-gray-100 hover:bg-gray-50/50 transition-colors">
      <td className="p-3">
        {post.postCacheFlag && (
          <Badge className="bg-orange-400 hover:bg-orange-500 text-white text-xs">
            {post.postCacheFlag === "REALTIME" && "실시간"}
            {post.postCacheFlag === "WEEKLY" && "주간"}
            {post.postCacheFlag === "LEGEND" && "레전드"}
          </Badge>
        )}
      </td>
      <td className="p-3">
        <div className="flex items-center gap-2">
          <Link
            href={`/board/post/${post.id}`}
            className={`font-semibold transition-colors line-clamp-2 block flex-1 ${
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
            <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0" title="읽음" />
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
      </td>
    <td className="p-3 text-brand-muted">
      <Link
        href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
        className="hover:text-purple-600 hover:underline transition-colors truncate block max-w-20"
        title={`${post.userName}님의 롤링페이퍼 보기`}
      >
        {post.userName}
      </Link>
    </td>
    <td className="p-3 text-brand-muted text-sm">
      {formatDate(post.createdAt)}
    </td>
    <td className="p-3 text-brand-muted text-center">{post.viewCount}</td>
    <td className="p-3 text-brand-muted text-center">{post.likeCount}</td>
  </tr>
));

// 모바일용 카드 컴포넌트
const PostListMobileItem = React.memo<PostListItemProps & { isRead: boolean; progress: number }>(
  ({ post, isRead, progress }) => (
    <Card variant="elevated" className="hover:shadow-brand-md transition-all">
      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <div className="flex-1">
            {post.postCacheFlag && (
              <Badge className="bg-orange-400 text-white text-xs mb-2">
                {post.postCacheFlag === "REALTIME" && "실시간"}
                {post.postCacheFlag === "WEEKLY" && "주간"}
                {post.postCacheFlag === "LEGEND" && "레전드"}
              </Badge>
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
                <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0 mt-1" title="읽음" />
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
      <Card variant="elevated" className="hidden sm:block">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr className="text-brand-muted">
                  <th className="p-3 text-left font-medium w-20">상태</th>
                  <th className="p-3 text-left font-medium">제목</th>
                  <th className="p-3 text-left font-medium w-24">작성자</th>
                  <th className="p-3 text-left font-medium w-28">작성일</th>
                  <th className="p-3 text-center font-medium w-16">조회</th>
                  <th className="p-3 text-center font-medium w-16">추천</th>
                </tr>
              </thead>
              <tbody>
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
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-brand-secondary">
                      게시글이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

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
