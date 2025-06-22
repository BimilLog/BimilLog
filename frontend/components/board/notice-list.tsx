"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Pin } from "lucide-react";

interface Notice {
  id: number;
  title: string;
  author: string;
  date: string;
  views: number;
  isPinned: boolean;
}

interface NoticeListProps {
  notices: Notice[];
}

export const NoticeList = ({ notices }: NoticeListProps) => {
  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <tbody>
              {notices.map((notice) => (
                <tr
                  key={notice.id}
                  className="border-b border-gray-100 bg-purple-50/50 hover:bg-purple-100/50 transition-colors"
                >
                  <td className="p-3 text-left font-medium hidden sm:table-cell w-20">
                    <div className="flex items-center text-purple-600">
                      <Pin className="w-4 h-4 mr-2" />
                      <span>공지</span>
                    </div>
                  </td>
                  <td className="p-3 text-left font-semibold text-purple-800">
                    {notice.title}
                  </td>
                  <td className="p-3 text-left font-medium w-32 hidden md:table-cell">
                    {notice.author}
                  </td>
                  <td className="p-3 text-left font-medium w-32 hidden md:table-cell">
                    {notice.date}
                  </td>
                  <td className="p-3 text-left font-medium w-16">
                    {notice.views}
                  </td>
                  <td className="p-3 text-left font-medium w-16">-</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
};
