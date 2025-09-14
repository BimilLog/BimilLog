import React from "react";
import { Button } from "@/components";
import { MessageSquare, Edit } from "lucide-react";
import Link from "next/link";

export const BoardHeader = React.memo(() => {
  return (
    <header className="py-8">
      <div className="container mx-auto px-4 text-center">
        <div className="flex items-center justify-center space-x-3 mb-4">
          <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-xl flex items-center justify-center">
            <MessageSquare className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
            커뮤니티 게시판
          </h1>
        </div>
        <p className="text-lg text-brand-muted max-w-2xl mx-auto mb-6">
          다른 사용자들과 소통하고 생각을 나누어보세요
        </p>
        <Button
          asChild
          size="lg"
          className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700"
        >
          <Link href="/board/write">
            <Edit className="w-5 h-5 mr-2" />
            글쓰기
          </Link>
        </Button>
      </div>
    </header>
  );
});

BoardHeader.displayName = "BoardHeader";
