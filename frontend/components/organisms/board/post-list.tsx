import React from "react";
import { Card, CardContent } from "@/components";
import { Badge } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";

interface PostListProps {
  posts: SimplePost[];
}

interface PostListItemProps {
  post: SimplePost;
}

// 데스크톱용 테이블 행 컴포넌트
const PostListTableItem = React.memo<PostListItemProps>(({ post }) => (
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
      <Link
        href={`/board/post/${post.id}`}
        className="font-semibold text-gray-800 hover:text-purple-600 transition-colors line-clamp-2 block"
      >
        {post.title}
        {post.commentCount > 0 && (
          <span className="ml-2 text-purple-500 font-normal">
            [{post.commentCount}]
          </span>
        )}
      </Link>
    </td>
    <td className="p-3 text-gray-600">
      <Link
        href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
        className="hover:text-purple-600 hover:underline transition-colors truncate block max-w-20"
        title={`${post.userName}님의 롤링페이퍼 보기`}
      >
        {post.userName}
      </Link>
    </td>
    <td className="p-3 text-gray-600 text-sm">
      {formatDate(post.createdAt)}
    </td>
    <td className="p-3 text-gray-600 text-center">{post.viewCount}</td>
    <td className="p-3 text-gray-600 text-center">{post.likeCount}</td>
  </tr>
));

// 모바일용 카드 컴포넌트
const PostListMobileItem = React.memo<PostListItemProps>(({ post }) => (
  <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-sm hover:shadow-md transition-all">
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
          <Link
            href={`/board/post/${post.id}`}
            className="font-semibold text-gray-800 hover:text-purple-600 transition-colors line-clamp-2 block text-base"
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

      <div className="flex items-center justify-between text-sm text-gray-500">
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

  return (
    <>
      {/* 태블릿 이상에서 테이블 형태로 표시 */}
      <Card className="hidden sm:block bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr className="text-gray-600">
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
                    <PostListTableItem key={post.id} post={post} />
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-12 text-gray-500">
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
            <PostListMobileItem key={post.id} post={post} />
          ))
        ) : (
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-sm">
            <div className="p-8 text-center text-gray-500">
              게시글이 없습니다.
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

PostList.displayName = "PostList";
