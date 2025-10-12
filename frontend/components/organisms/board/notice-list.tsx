import { Card, CardContent, Badge, Button } from "@/components";
import { Eye, Megaphone, ThumbsUp, User, ExternalLink } from "lucide-react";
import Link from "next/link";
import type { SimplePost } from "@/lib/api";
import { formatKoreanDate } from "@/lib/utils/date";
import { Popover } from "flowbite-react";

interface NoticeListProps {
  posts: SimplePost[];
}

const numberFormatter = new Intl.NumberFormat("ko-KR");

export const NoticeList = ({ posts }: NoticeListProps) => {
  // /api/post/notice 엔드포인트에서 가져온 공지사항 전용 데이터
  if (posts.length === 0) {
    return null;
  }

  return (
    <>
      <Card variant="elevated" className="hidden sm:block">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full table-fixed text-sm md:text-base">
            <thead className="bg-purple-50 text-purple-600 dark:bg-[#1a1030] dark:text-purple-200">
              <tr className="text-left">
                <th className="w-20 px-4 py-3 text-center">구분</th>
                <th className="px-4 py-3">제목</th>
                <th className="w-28 px-4 py-3 text-center hidden sm:table-cell">
                  작성일
                </th>
                <th className="w-28 pl-10 pr-4 py-3 text-left">
                  작성자
                </th>
                <th className="w-24 px-4 py-3 text-center">추천</th>
                <th className="w-24 px-4 py-3 text-center">조회</th>
              </tr>
            </thead>
            <tbody>
              {posts.map((notice) => {
                const trimmedName = notice.memberName?.trim();
                const authorName =
                  trimmedName && trimmedName.length > 0 ? trimmedName : "익명";
                const hasAuthorLink = authorName !== "익명";

                return (
                  <tr
                    key={notice.id}
                    className="border-b border-gray-100 bg-white transition-colors hover:bg-purple-50/60 dark:border-slate-800 dark:bg-slate-900/70 dark:hover:bg-[#201b3d]"
                  >
                    <td className="px-4 py-3 text-center">
                      <Badge
                        variant="info"
                        icon={Megaphone}
                        className="inline-flex"
                      >
                        공지
                      </Badge>
                    </td>
                    <td className="px-4 py-3 align-middle">
                      <Link
                        href={`/board/post/${notice.id}`}
                        className="group flex items-center gap-2 font-semibold text-slate-900 hover:text-purple-600 dark:text-gray-100 dark:hover:text-purple-300"
                      >
                        <span className="line-clamp-1">
                          {notice.title}
                          {notice.commentCount > 0 && (
                            <span className="ml-2 text-purple-500 font-normal">
                              [{notice.commentCount}]
                            </span>
                          )}
                        </span>
                      </Link>
                    </td>
                    <td className="hidden px-4 py-3 text-center text-sm text-slate-500 dark:text-gray-400 sm:table-cell">
                      {formatKoreanDate(notice.createdAt)}
                    </td>
                    <td className="pl-10 pr-4 py-3 text-left">
                      {hasAuthorLink ? (
                        <Link
                          href={`/rolling-paper/${encodeURIComponent(authorName)}`}
                          className="inline-flex items-center gap-1 whitespace-nowrap text-sm text-slate-600 hover:text-purple-600 hover:underline dark:text-gray-300 dark:hover:text-purple-300"
                          title={`${authorName}님의 롤링페이퍼 보기`}
                        >
                          <User className="h-4 w-4" />
                          {authorName}
                        </Link>
                      ) : (
                        <span className="inline-flex items-center gap-1 whitespace-nowrap text-sm text-slate-500 dark:text-gray-400">
                          <User className="h-4 w-4" />
                          익명
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center text-slate-600 dark:text-gray-300">
                      <span className="inline-flex items-center justify-center gap-1 whitespace-nowrap">
                        <ThumbsUp className="h-4 w-4" />
                        {numberFormatter.format(notice.likeCount)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center text-slate-600 dark:text-gray-300">
                      <span className="flex items-center justify-center gap-1">
                        <Eye className="h-4 w-4" />
                        {numberFormatter.format(notice.viewCount)}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
      <div className="sm:hidden space-y-3 overflow-visible">
      {posts.map((notice, index) => {
        const trimmedName = notice.memberName?.trim();
        const authorName =
          trimmedName && trimmedName.length > 0 ? trimmedName : "익명";
        const hasAuthorLink = authorName !== "익명";

        return (
          <div
            key={notice.id}
            style={{ zIndex: posts.length - index }}
            className="relative"
          >
          <Card
            variant="elevated"
            className="relative p-3 gap-1 overflow-visible shadow-sm isolation-auto dark:shadow-none"
          >
            <div className="flex items-center justify-between text-xs text-purple-500 dark:text-purple-300">
              <Badge variant="info" icon={Megaphone}>
                공지
              </Badge>
              <span>{formatKoreanDate(notice.createdAt)}</span>
            </div>
            <Link
              href={`/board/post/${notice.id}`}
              className="mt-2 block text-base font-semibold text-slate-900 hover:text-purple-600 dark:text-gray-100 dark:hover:text-purple-300"
            >
              {notice.title}
              {notice.commentCount > 0 && (
                <span className="ml-2 text-purple-500 font-normal">
                  [{notice.commentCount}]
                </span>
              )}
            </Link>
            <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600 dark:text-gray-300">
              {hasAuthorLink ? (
                <Popover
                  trigger="click"
                  placement="bottom"
                  theme={{
                    base: "z-[9999] absolute",
                  }}
                  content={
                    <div className="w-56 p-3">
                      <div className="flex flex-col space-y-2">
                        <div className="flex items-center space-x-2">
                          <User className="h-4 w-4" />
                          <span className="font-medium">{authorName}</span>
                        </div>
                        <Link href={`/rolling-paper/${encodeURIComponent(authorName)}`}>
                          <Button size="sm" className="w-full justify-start">
                            <ExternalLink className="mr-2 h-4 w-4" />
                            롤링페이퍼 보기
                          </Button>
                        </Link>
                      </div>
                    </div>
                  }
                >
                  <button
                    type="button"
                    className="inline-flex items-center gap-1 text-left transition-colors hover:text-purple-600 hover:underline dark:text-gray-200 dark:hover:text-purple-300"
                    title={`${authorName}님의 롤링페이퍼 보기`}
                  >
                    <User className="h-4 w-4" />
                    <span>{authorName}</span>
                  </button>
                </Popover>
              ) : (
                <span className="inline-flex items-center gap-1 text-slate-500 dark:text-gray-400">
                  <User className="h-4 w-4" />
                  익명
                </span>
              )}
              <div className="flex items-center gap-3 text-sm">
                <span className="inline-flex items-center gap-1">
                  <ThumbsUp className="h-4 w-4" />
                  {numberFormatter.format(notice.likeCount)}
                </span>
                <span className="inline-flex items-center gap-1">
                  <Eye className="h-4 w-4" />
                  {numberFormatter.format(notice.viewCount)}
                </span>
              </div>
            </div>
          </Card>
          </div>
        );
      })}
      </div>
    </>
  );
};
