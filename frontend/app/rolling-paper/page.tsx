"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { rollingPaperApi, type RollingPaperMessage } from "@/lib/api";
import { useRouter } from "next/navigation";
import { MessageSquare } from "lucide-react";

// 분리된 컴포넌트들 import
import { RollingPaperHeader } from "./components/RollingPaperHeader";
import { RollingPaperGrid } from "./components/RollingPaperGrid";
import { RecentMessages } from "./components/RecentMessages";

export default function RollingPaperPage() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [selectedCell, setSelectedCell] = useState<number | null>(null);
  const [messages, setMessages] = useState<{
    [key: number]: RollingPaperMessage;
  }>({});
  const [isLoadingMessages, setIsLoadingMessages] = useState(true);

  // 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, isLoading, router]);

  // 내 롤링페이퍼 메시지 조회
  useEffect(() => {
    const fetchMessages = async () => {
      if (!isAuthenticated || !user) return;

      try {
        const response = await rollingPaperApi.getMyRollingPaper();
        if (response.success && response.data) {
          const messageMap: { [key: number]: RollingPaperMessage } = {};
          response.data.forEach((message) => {
            const position = message.height * 7 + message.width;
            messageMap[position] = message;
          });
          setMessages(messageMap);
        }
      } catch (error) {
        console.error("Failed to fetch messages:", error);
      } finally {
        setIsLoadingMessages(false);
      }
    };

    fetchMessages();
  }, [isAuthenticated, user]);

  if (isLoading || isLoadingMessages) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600">롤링페이퍼를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return null;
  }

  const handleMessageSubmit = (index: number, data: any) => {
    setMessages((prev) => ({
      ...prev,
      [index]: {
        id: Date.now(),
        userId: user?.userId || 0,
        decoType: data.decoType,
        anonymity: data.anonymousNickname,
        content: data.content,
        width: index % 7,
        height: Math.floor(index / 7),
        createdAt: new Date().toISOString(),
        isDeleted: false,
      },
    }));
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <RollingPaperHeader
        user={user}
        messageCount={Object.keys(messages).length}
      />

      <div className="container mx-auto px-4 py-8">
        <RollingPaperGrid
          messages={messages}
          onMessageSubmit={handleMessageSubmit}
        />

        <RecentMessages messages={messages} />
      </div>
    </div>
  );
}
