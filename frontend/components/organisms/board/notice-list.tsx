import { Card, CardContent, Badge } from "@/components";
import { Megaphone, User } from "lucide-react";
import Link from "next/link";
import type { SimplePost } from "@/lib/api";
import { formatKoreanDate } from "@/lib/utils/date";

interface NoticeListProps {
  posts: SimplePost[];
}

export const NoticeList = ({ posts }: NoticeListProps) => {
  const notices = posts.filter((post) => post.isNotice);

  if (notices.length === 0) {
    return null;
  }

  return (
    <Card variant="elevated">
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-base md:text-sm">
            <tbody>
              {notices.map((notice) => (
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
                        notice.userName
                      )}`}
                      className="hover:text-purple-600 hover:underline transition-colors inline-flex items-center space-x-1"
                      title={`${notice.userName}님의 롤링페이퍼 보기`}
                    >
                      <User className="w-3 h-3" />
                      <span>{notice.userName}</span>
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
