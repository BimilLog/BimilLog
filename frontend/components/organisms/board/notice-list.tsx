import { Card, CardContent, Badge } from "@/components";
import { Megaphone, User } from "lucide-react";
import Link from "next/link";
import type { SimplePost } from "@/lib/api";
import { formatKoreanDate } from "@/lib/utils/date";

interface NoticeListProps {
  posts: SimplePost[];
}

export const NoticeList = ({ posts }: NoticeListProps) => {
  // 공지사항 API로 받은 데이터는 모두 공지사항이므로 별도 필터링 불필요
  if (posts.length === 0) {
    return null;
  }

  return (
    <Card variant="elevated">
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-base md:text-sm">
            <tbody>
              {posts.map((notice) => (
                <tr
                  key={notice.id}
                  className="border-b border-gray-100 bg-purple-50/50 hover:bg-purple-100/50 transition-colors"
                >
                  <td className="p-4 md:p-3 text-left font-medium hidden sm:table-cell w-20">
                    <Badge variant="info" icon={Megaphone}>공지</Badge>
                  </td>
                  <td className="p-4 md:p-3 text-left font-semibold text-purple-800">
                    <Link
                      href={`/board/post/${notice.id}`}
                      className="hover:underline"
                    >
                      {notice.title}
                    </Link>
                  </td>
                  <td className="p-4 md:p-3 text-left font-medium w-32 hidden md:table-cell">
                    <Link
                      href={`/rolling-paper/${encodeURIComponent(
                        notice.memberName
                      )}`}
                      className="hover:text-purple-600 hover:underline transition-colors inline-flex items-center space-x-1"
                      title={`${notice.memberName}님의 롤링페이퍼 보기`}
                    >
                      <User className="w-3 h-3 stroke-slate-600 fill-slate-100" />
                      <span>{notice.memberName}</span>
                    </Link>
                  </td>
                  <td className="p-4 md:p-3 text-left font-medium w-32 hidden md:table-cell">
                    {formatKoreanDate(notice.createdAt)}
                  </td>
                  <td className="p-4 md:p-3 text-left font-medium w-16">
                    {notice.viewCount}
                  </td>
                  <td className="p-4 md:p-3 text-left font-medium w-16">
                    {notice.likeCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
};
