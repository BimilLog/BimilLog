"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils/date";
import { ThumbsUp, MessageCircle, Eye, TrendingUp, Users, Activity } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow, Popover } from "flowbite-react";

interface PopularPostListProps {
  posts: SimplePost[];
  title: string;
  icon: React.ReactNode;
}

export const PopularPostList = ({
  posts,
  title,
  icon,
}: PopularPostListProps) => {
  const router = useRouter();

  const handleAuthorClick = (e: React.MouseEvent, userName: string) => {
    e.preventDefault();
    e.stopPropagation();
    router.push(`/rolling-paper/${encodeURIComponent(userName)}`);
  };

  return (
    <Card variant="elevated">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          {icon}
          <span>{title}</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        {posts.length > 0 ? (
          <div className="overflow-x-auto">
            <Table hoverable>
              <TableHead>
                <TableRow>
                  <TableHeadCell className="w-12 text-center">순위</TableHeadCell>
                  <TableHeadCell>제목</TableHeadCell>
                  <TableHeadCell className="w-24">작성자</TableHeadCell>
                  <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
                  <TableHeadCell className="w-16 text-center">추천</TableHeadCell>
                  <TableHeadCell className="w-16 text-center">댓글</TableHeadCell>
                </TableRow>
              </TableHead>
              <TableBody className="divide-y">
                {posts.map((post, index) => (
                  <TableRow key={post.id} className="bg-white hover:bg-gray-50">
                    <TableCell className="text-center">
                      <span className="text-lg font-bold text-purple-600">
                        {index + 1}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Link
                        href={`/board/post/${post.id}`}
                        className="font-semibold text-gray-900 hover:text-purple-600 transition-colors block"
                      >
                        {post.title}
                        {post.commentCount > 0 && (
                          <span className="ml-2 text-purple-500 font-normal">
                            [{post.commentCount}]
                          </span>
                        )}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <button
                        className="hover:text-purple-600 hover:underline transition-colors text-left truncate block max-w-20"
                        title={`${post.userName}님의 롤링페이퍼 보기`}
                        onClick={(e) => handleAuthorClick(e, post.userName)}
                      >
                        {post.userName}
                      </button>
                    </TableCell>
                    <TableCell className="hidden sm:table-cell text-sm">
                      {formatDate(post.createdAt)}
                    </TableCell>
                    <TableCell className="text-center">
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
                              <div className="flex justify-between">
                                <span className="text-gray-500">순위:</span>
                                <span className="font-medium text-purple-600">#{index + 1}</span>
                              </div>
                            </div>
                          </div>
                        }
                      >
                        <span className="flex items-center justify-center cursor-help">
                          <ThumbsUp className="w-3 h-3 mr-1" />
                          {post.likeCount}
                        </span>
                      </Popover>
                    </TableCell>
                    <TableCell className="text-center">
                      <Popover
                        trigger="hover"
                        placement="top"
                        content={
                          <div className="p-3 min-w-[180px]">
                            <div className="flex items-center gap-2 mb-2">
                              <MessageCircle className="w-4 h-4 text-blue-600" />
                              <span className="font-semibold text-sm">댓글 통계</span>
                            </div>
                            <div className="space-y-1 text-xs">
                              <div className="flex justify-between">
                                <span className="text-gray-500">총 댓글:</span>
                                <span className="font-medium">{post.commentCount}개</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-gray-500">활발도:</span>
                                <span className="font-medium">
                                  {post.commentCount > 10 ? '활발' : post.commentCount > 5 ? '보통' : '조용'}
                                </span>
                              </div>
                            </div>
                          </div>
                        }
                      >
                        <span className="flex items-center justify-center cursor-help">
                          <MessageCircle className="w-3 h-3 mr-1" />
                          {post.commentCount}
                        </span>
                      </Popover>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : (
          <div className="text-center py-12 text-gray-500">
            인기글이 없습니다.
          </div>
        )}
      </CardContent>
    </Card>
  );
};
