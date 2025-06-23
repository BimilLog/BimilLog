"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  MessageSquare,
  Plus,
  Heart,
  Share2,
  ArrowLeft,
  Send,
} from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  rollingPaperApi,
  getDecoInfo,
  type VisitMessage,
  type RollingPaperMessage,
} from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";

export default function PublicRollingPaperPage() {
  const params = useParams();
  const { user, isAuthenticated } = useAuth();
  const [nickname, setNickname] = useState<string>("");
  const [messages, setMessages] = useState<{
    [key: number]: VisitMessage | RollingPaperMessage;
  }>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isOwner, setIsOwner] = useState(false);

  // params에서 nickname 추출
  useEffect(() => {
    if (params.nickname) {
      setNickname(params.nickname as string);
    }
  }, [params]);

  // 롤링페이퍼 메시지 조회
  useEffect(() => {
    const fetchMessages = async () => {
      try {
        const response = await rollingPaperApi.getRollingPaper(nickname);
        if (response.success && response.data) {
          const messageMap: { [key: number]: VisitMessage } = {};
          response.data.forEach((message) => {
            const position = message.height * 6 + message.width; // 6칸으로 변경
            messageMap[position] = message;
          });
          setMessages(messageMap);
        }
      } catch (error) {
        console.error("Failed to fetch messages:", error);
      } finally {
        setIsLoading(false);
      }
    };

    if (nickname) {
      fetchMessages();
    }
  }, [nickname]);

  // 소유자 확인
  useEffect(() => {
    if (isAuthenticated && user && nickname) {
      setIsOwner(user.userName === decodeURIComponent(nickname));
    }
  }, [isAuthenticated, user, nickname]);

  const handleShare = async () => {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({
          title: `${nickname}님의 롤링페이퍼`,
          text: "익명으로 따뜻한 메시지를 남겨보세요!",
          url: url,
        });
      } catch (error) {
        console.log("Share cancelled");
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
        alert("링크가 클립보드에 복사되었습니다!");
      } catch (error) {
        console.error("Failed to copy to clipboard:", error);
      }
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600 font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                홈으로
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
                <MessageSquare className="w-5 h-5 text-white" />
              </div>
              <div>
                <h1 className="font-bold text-gray-800">
                  {decodeURIComponent(nickname)}님의 롤링페이퍼
                </h1>
                <p className="text-xs text-gray-500">
                  총 {Object.keys(messages).length}개의 메시지
                </p>
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              className="bg-white"
              onClick={handleShare}
            >
              <Share2 className="w-4 h-4 mr-2" />
              공유하기
            </Button>
            <Button variant="outline" size="sm" className="bg-white">
              <Heart className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Rolling Paper Grid - 귀여운 종이 디자인 */}
        <div className="relative max-w-4xl mx-auto mb-8">
          {/* 종이 배경 */}
          <div
            className="relative bg-gradient-to-br from-amber-50 via-yellow-50 to-orange-50 rounded-3xl shadow-2xl border-4 border-pink-200"
            style={{
              backgroundImage: `
                radial-gradient(circle at 20px 20px, rgba(255,182,193,0.3) 1px, transparent 1px),
                radial-gradient(circle at 80px 80px, rgba(255,192,203,0.2) 1px, transparent 1px),
                linear-gradient(45deg, rgba(255,255,255,0.1) 25%, transparent 25%),
                linear-gradient(-45deg, rgba(255,255,255,0.1) 25%, transparent 25%)
              `,
              backgroundSize: "40px 40px, 160px 160px, 20px 20px, 20px 20px",
            }}
          >
            {/* 바인더 구멍들 */}
            <div className="absolute left-6 top-0 bottom-0 flex flex-col justify-evenly">
              {Array.from({ length: 12 }, (_, i) => (
                <div
                  key={i}
                  className="w-6 h-6 bg-white rounded-full shadow-inner border-2 border-pink-300"
                  style={{
                    boxShadow:
                      "inset 0 2px 4px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.1)",
                  }}
                />
              ))}
            </div>

            {/* 제목 영역 */}
            <div className="pt-8 pb-6 px-20 text-center">
              <div className="relative">
                {/* 현대적이면서 귀여운 제목 */}
                <h1 className="text-3xl font-bold text-pink-800 mb-2 transform -rotate-1">
                  💕 {decodeURIComponent(nickname)}님의 롤링페이퍼 💕
                </h1>

                {/* 귀여운 데코레이션 */}
                <div className="absolute -top-2 -left-4 text-2xl animate-bounce">
                  🌸
                </div>
                <div className="absolute -top-1 -right-6 text-xl animate-pulse">
                  ✨
                </div>
                <div className="absolute -bottom-2 left-8 text-lg animate-bounce delay-300">
                  🎀
                </div>
                <div className="absolute -bottom-1 right-12 text-xl animate-pulse delay-500">
                  💖
                </div>

                <p className="text-pink-600 text-sm mt-2 transform rotate-1 font-medium">
                  총 {Object.keys(messages).length}개의 따뜻한 메시지 💌
                </p>
              </div>
            </div>

            {/* 메시지 그리드 - 6칸으로 변경 */}
            <div className="px-20 pb-12">
              <div className="grid grid-cols-6 gap-3 bg-white/30 p-6 rounded-2xl border-2 border-dashed border-pink-300">
                {Array.from({ length: 84 }, (_, i) => {
                  // 6x14 = 84칸
                  const hasMessage = messages[i];
                  const decoInfo = hasMessage
                    ? getDecoInfo(hasMessage.decoType)
                    : null;

                  return (
                    <Dialog key={i}>
                      <DialogTrigger asChild>
                        <div
                          className={`
                            aspect-square rounded-xl border-3 flex items-center justify-center cursor-pointer transition-all duration-300 transform hover:scale-110
                            ${
                              hasMessage
                                ? `bg-gradient-to-br ${decoInfo?.color} border-white shadow-lg hover:shadow-xl hover:rotate-3`
                                : "border-dashed border-pink-300 hover:border-pink-500 hover:bg-pink-50 hover:rotate-2"
                            }
                          `}
                          style={{
                            boxShadow: hasMessage
                              ? "0 4px 15px rgba(255,182,193,0.4), inset 0 1px 0 rgba(255,255,255,0.5)"
                              : "0 2px 8px rgba(255,182,193,0.2)",
                          }}
                        >
                          {hasMessage ? (
                            <div className="relative">
                              <span className="text-2xl animate-bounce">
                                {decoInfo?.emoji}
                              </span>
                              {/* 반짝이는 효과 */}
                              <div className="absolute -top-1 -right-1 w-2 h-2 bg-yellow-300 rounded-full animate-ping"></div>
                            </div>
                          ) : (
                            <div className="relative group">
                              <Plus className="w-5 h-5 text-pink-400 group-hover:text-pink-600 transition-colors" />
                              <div className="absolute inset-0 bg-pink-200 rounded-full opacity-0 group-hover:opacity-30 transition-opacity animate-pulse"></div>
                            </div>
                          )}
                        </div>
                      </DialogTrigger>
                      <DialogContent className="max-w-md mx-4 bg-gradient-to-br from-pink-50 to-purple-50 border-4 border-pink-200 rounded-3xl">
                        <DialogHeader>
                          <DialogTitle className="text-center text-pink-800 font-bold">
                            {hasMessage
                              ? "💌 메시지 보기"
                              : "✨ 새 메시지 작성"}
                          </DialogTitle>
                        </DialogHeader>
                        {hasMessage ? (
                          <MessageView message={hasMessage} isOwner={isOwner} />
                        ) : (
                          <MessageForm
                            nickname={nickname}
                            position={{ x: i % 6, y: Math.floor(i / 6) }} // 6칸으로 변경
                            onSubmit={(newMessage) => {
                              setMessages((prev) => ({
                                ...prev,
                                [i]: newMessage,
                              }));
                            }}
                          />
                        )}
                      </DialogContent>
                    </Dialog>
                  );
                })}
              </div>
            </div>

            {/* 귀여운 스티커들 */}
            <div className="absolute top-16 right-8 text-3xl animate-spin-slow">
              🌟
            </div>
            <div className="absolute top-32 left-12 text-2xl animate-bounce">
              🦋
            </div>
            <div className="absolute bottom-20 right-16 text-2xl animate-pulse">
              🌺
            </div>
            <div className="absolute bottom-32 left-8 text-xl animate-bounce delay-700">
              🍀
            </div>
          </div>
        </div>

        {/* Recent Messages */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl rounded-3xl border-4 border-pink-200">
          <CardHeader className="bg-gradient-to-r from-pink-100 to-purple-100 rounded-t-3xl">
            <CardTitle className="flex items-center space-x-2 text-pink-800">
              <MessageSquare className="w-5 h-5" />
              <span className="font-bold">최근 메시지들 💕</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="space-y-4">
              {Object.values(messages)
                .slice(-3)
                .map((message) => {
                  const decoInfo = getDecoInfo(message.decoType);
                  return (
                    <div
                      key={message.id}
                      className="flex items-start space-x-3 p-4 rounded-2xl bg-gradient-to-r from-pink-50 to-purple-50 border-2 border-pink-200 transform hover:scale-105 transition-transform"
                    >
                      <div
                        className={`w-12 h-12 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center shadow-lg border-2 border-white`}
                      >
                        <span className="text-xl">{decoInfo.emoji}</span>
                      </div>
                      <div className="flex-1">
                        <p className="text-gray-800 text-sm font-medium">
                          {"content" in message
                            ? message.content
                            : "누군가 따뜻한 메시지를 남겼어요 💕"}
                        </p>
                        <div className="flex items-center space-x-2 mt-2">
                          <Badge
                            variant="outline"
                            className="text-xs bg-white border-pink-300"
                          >
                            {"anonymity" in message
                              ? message.anonymity
                              : "익명"}
                          </Badge>
                          <span className="text-xs text-gray-500 font-medium">
                            {decoInfo.name}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              {Object.keys(messages).length === 0 && (
                <div className="text-center py-12">
                  <div className="text-6xl mb-4">📝</div>
                  <p className="text-gray-500 text-lg font-semibold">
                    아직 메시지가 없어요
                  </p>
                  <p className="text-gray-400 text-sm mt-2 font-medium">
                    첫 번째 메시지를 남겨보세요! 💌
                  </p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function MessageView({
  message,
  isOwner,
}: {
  message: VisitMessage | RollingPaperMessage;
  isOwner: boolean;
}) {
  const decoInfo = getDecoInfo(message.decoType);

  const handleDelete = async () => {
    if (!isOwner || !("content" in message)) return;

    if (confirm("정말로 이 메시지를 삭제하시겠습니까?")) {
      try {
        const response = await rollingPaperApi.deleteMessage({
          id: message.id,
          userId: message.userId,
          decoType: message.decoType,
          anonymity: message.anonymity,
          content: message.content,
          width: message.width,
          height: message.height,
        });
        if (response.success) {
          window.location.reload();
        }
      } catch (error) {
        console.error("Failed to delete message:", error);
        alert("메시지 삭제에 실패했습니다.");
      }
    }
  };

  return (
    <div className="space-y-4">
      <div
        className={`p-6 rounded-2xl bg-gradient-to-br ${decoInfo.color} border-4 border-white shadow-xl relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 10px 10px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 30px 30px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "20px 20px, 60px 60px",
        }}
      >
        <div className="flex items-center space-x-3 mb-4">
          <span className="text-4xl animate-bounce">{decoInfo.emoji}</span>
          <Badge
            variant="secondary"
            className="bg-white/80 text-pink-800 border-pink-300 font-semibold"
          >
            {decoInfo.name}
          </Badge>
        </div>
        {"content" in message ? (
          <p className="text-gray-800 leading-relaxed font-medium">
            {message.content}
          </p>
        ) : (
          <p className="text-gray-500 italic font-medium">
            메시지 내용을 볼 수 없습니다
          </p>
        )}

        {/* 반짝이는 효과 */}
        <div className="absolute top-2 right-2 w-3 h-3 bg-yellow-300 rounded-full animate-ping"></div>
        <div className="absolute bottom-3 left-3 w-2 h-2 bg-pink-300 rounded-full animate-pulse delay-500"></div>
      </div>

      <div className="flex items-center justify-between">
        <div>
          {"anonymity" in message && (
            <Badge
              variant="outline"
              className="bg-pink-50 border-pink-300 text-pink-800 font-semibold"
            >
              {message.anonymity}
            </Badge>
          )}
        </div>
        {isOwner && "content" in message && (
          <Button
            variant="outline"
            size="sm"
            className="text-red-600 border-red-200 hover:bg-red-50 rounded-full font-semibold"
            onClick={handleDelete}
          >
            삭제
          </Button>
        )}
      </div>
    </div>
  );
}

const decoTypeMap = {
  POTATO: { name: "감자", emoji: "🥔", color: "from-yellow-400 to-orange-500" },
  SWEET_POTATO: {
    name: "고구마",
    emoji: "🍠",
    color: "from-orange-500 to-red-600",
  },
  CHESTNUT: { name: "밤", emoji: "🌰", color: "from-amber-600 to-yellow-700" },
  PEANUT: { name: "땅콩", emoji: "🥜", color: "from-yellow-700 to-yellow-800" },
  ACORN: {
    name: "도토리",
    emoji: "🌰",
    color: "from-orange-800 to-yellow-900",
  },
};

function MessageForm({
  nickname,
  position,
  onSubmit,
}: {
  nickname: string;
  position: { x: number; y: number };
  onSubmit: (message: any) => void;
}) {
  const [content, setContent] = useState("");
  const [anonymity, setAnonymity] = useState("");
  const [decoType, setDecoType] = useState("POTATO");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const decoOptions = Object.entries(getDecoInfo).map(([key, info]) => ({
    value: key,
    label: `${info.emoji} ${info.name}`,
    info,
  }));

  const handleSubmit = async () => {
    if (!content.trim() || !anonymity.trim()) {
      alert("모든 필드를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await rollingPaperApi.createMessage(nickname, {
        decoType,
        anonymity: anonymity.trim(),
        content: content.trim(),
        width: position.x,
        height: position.y,
      });

      if (response.success) {
        onSubmit({
          id: Date.now(),
          userId: 0,
          decoType,
          anonymity: anonymity.trim(),
          content: content.trim(),
          width: position.x,
          height: position.y,
        });
        setContent("");
        setAnonymity("");
        alert("메시지가 성공적으로 등록되었습니다! 💌");
      } else {
        alert("메시지 등록에 실패했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("Failed to create message:", error);
      alert("메시지 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedDecoInfo = getDecoInfo(decoType);

  return (
    <div className="space-y-6">
      <div>
        <label className="block text-sm font-bold mb-3 text-pink-800">
          💭 익명 닉네임
        </label>
        <Input
          placeholder="익명의 친구"
          value={anonymity}
          onChange={(e) => setAnonymity(e.target.value)}
          className="border-3 border-pink-300 rounded-2xl focus:border-pink-500 bg-pink-50 font-medium"
        />
      </div>

      <div>
        <label className="block text-sm font-bold mb-3 text-pink-800">
          💌 따뜻한 메시지
        </label>
        <Textarea
          placeholder="따뜻한 메시지를 남겨주세요..."
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={4}
          className="border-3 border-pink-300 rounded-2xl focus:border-pink-500 bg-pink-50 resize-none font-medium"
        />
      </div>

      <div>
        <label className="block text-sm font-bold mb-3 text-pink-800">
          🎨 데코레이션 선택
        </label>
        <Select value={decoType} onValueChange={setDecoType}>
          <SelectTrigger className="border-3 border-pink-300 rounded-2xl focus:border-pink-500 bg-pink-50">
            <SelectValue>
              <div className="flex items-center space-x-2">
                <span className="text-xl">{selectedDecoInfo.emoji}</span>
                <span className="font-semibold">{selectedDecoInfo.name}</span>
              </div>
            </SelectValue>
          </SelectTrigger>
          <SelectContent className="rounded-2xl border-3 border-pink-300">
            {Object.entries(decoTypeMap).map(([key, info]) => (
              <SelectItem key={key} value={key} className="rounded-xl">
                <div className="flex items-center space-x-2">
                  <span className="text-xl">{info.emoji}</span>
                  <span className="font-semibold">{info.name}</span>
                </div>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button
        onClick={handleSubmit}
        className="w-full h-12 bg-gradient-to-r from-pink-400 to-purple-500 hover:from-pink-500 hover:to-purple-600 text-white font-bold rounded-2xl shadow-lg transform hover:scale-105 transition-all"
        disabled={isSubmitting}
      >
        {isSubmitting ? (
          <div className="flex items-center space-x-2">
            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            <span>등록 중...</span>
          </div>
        ) : (
          <div className="flex items-center space-x-2">
            <Send className="w-5 h-5" />
            <span>메시지 남기기 💕</span>
          </div>
        )}
      </Button>
    </div>
  );
}
