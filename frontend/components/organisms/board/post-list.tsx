"use client";

import React from "react";
import { Card, CardContent } from "@/components";
import { Badge } from "@/components";
import { Button } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useState } from "react";
import { User } from "lucide-react";

interface PostListProps {
  posts: SimplePost[];
}

export const PostList = React.memo(({ posts }: PostListProps) => {
  const regularPosts = posts.filter((post) => !post.isNotice);

  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-base md:text-sm">
            <thead className="bg-gray-50">
              <tr className="text-gray-600">
                <th className="p-4 md:p-3 text-left font-medium hidden sm:table-cell w-20">
                  상태
                </th>
                <th className="p-4 md:p-3 text-left font-medium">제목</th>
                <th className="p-4 md:p-3 text-left font-medium w-32 hidden md:table-cell">
                  작성자
                </th>
                <th className="p-4 md:p-3 text-left font-medium w-32 hidden md:table-cell">
                  작성일
                </th>
                <th className="p-4 md:p-3 text-left font-medium w-16">조회</th>
                <th className="p-4 md:p-3 text-left font-medium w-16">추천</th>
              </tr>
            </thead>
            <tbody>
              {regularPosts.length > 0 ? (
                regularPosts.map((post) => (
                  <tr
                    key={post.id}
                    className="border-b border-gray-100 hover:bg-gray-50/50 transition-colors"
                  >
                    <td className="p-4 md:p-3 hidden sm:table-cell">
                      {post.postCacheFlag && (
                        <Badge className="bg-orange-400 hover:bg-orange-500 text-white">
                          {post.postCacheFlag === "REALTIME" && "실시간"}
                          {post.postCacheFlag === "WEEKLY" && "주간"}
                          {post.postCacheFlag === "LEGEND" && "레전드"}
                        </Badge>
                      )}
                    </td>
                    <td className="p-4 md:p-3">
                      <Link
                        href={`/board/post/${post.id}`}
                        className="font-semibold text-gray-800 hover:text-purple-600 transition-colors"
                      >
                        {post.title}
                        {post.commentCount > 0 && (
                          <span className="ml-2 text-purple-500 font-normal">
                            [{post.commentCount}]
                          </span>
                        )}
                      </Link>
                    </td>
                    <td className="p-4 md:p-3 text-gray-600 hidden md:table-cell">
                      <Link
                        href={`/rolling-paper/${encodeURIComponent(
                          post.userName
                        )}`}
                        className="hover:text-purple-600 hover:underline transition-colors"
                        title={`${post.userName}님의 롤링페이퍼 보기`}
                      >
                        <span>{post.userName}</span>
                      </Link>
                    </td>
                    <td className="p-4 md:p-3 text-gray-600 hidden md:table-cell">
                      {formatDate(post.createdAt)}
                    </td>
                    <td className="p-4 md:p-3 text-gray-600">{post.viewCount}</td>
                    <td className="p-4 md:p-3 text-gray-600">{post.likeCount}</td>
                  </tr>
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
  );
});

PostList.displayName = "PostList";
