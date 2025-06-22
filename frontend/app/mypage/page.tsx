"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import {
  userApi,
  rollingPaperApi,
  type SimplePost,
  type SimpleComment,
} from "@/lib/api";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  Edit,
  Heart,
  FileText,
  MessageCircle,
  ThumbsUp,
  Mail,
} from "lucide-react";
import Link from "next/link";
import { AuthHeader } from "@/components/auth-header";

const ActivityTabContent = ({
  fetchData,
}: {
  fetchData: () => Promise<any[]>;
}) => {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchData()
      .then((data) => setItems(data))
      .catch((err) => console.error("Failed to fetch activity data:", err))
      .finally(() => setIsLoading(false));
  }, [fetchData]);

  if (isLoading)
    return <div className="p-4 text-center">데이터를 불러오는 중...</div>;
  if (items.length === 0)
    return <div className="p-4 text-center">활동 내역이 없습니다.</div>;

  return (
    <div className="space-y-4 mt-4">
      {items.map((item) => (
        <Card
          key={item.id || item.postId}
          className="bg-white/80 backdrop-blur-sm"
        >
          <CardContent className="p-4">
            <Link href={`/board/post/${item.postId || item.id}`}>
              <div className="hover:text-purple-600 transition-colors">
                {item.title && <p className="font-semibold">{item.title}</p>}
                {item.content && (
                  <p className="text-gray-600">"{item.content}"</p>
                )}
                <p className="text-sm text-gray-400 mt-2">
                  {new Date(item.createdAt).toLocaleDateString()}
                </p>
              </div>
            </Link>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};

export default function MyPage() {
  const { user, isAuthenticated, isLoading, updateUserName, refreshUser } =
    useAuth();
  const router = useRouter();

  const [userStats, setUserStats] = useState({
    totalMessages: 0,
    totalPosts: 0,
    totalComments: 0,
    totalLikedPosts: 0,
    totalLikedComments: 0,
  });
  const [nicknameInput, setNicknameInput] = useState("");
  const [isNicknameChangeSubmitting, setIsNicknameChangeSubmitting] =
    useState(false);
  const [isNicknameDialogOpen, setIsNicknameDialogOpen] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
      return;
    }
    if (user) {
      setNicknameInput(user.userName);
      // Fetch user stats
      Promise.all([
        userApi.getUserPosts(0, 1),
        userApi.getUserComments(0, 1),
        userApi.getUserLikedPosts(0, 1),
        userApi.getUserLikedComments(0, 1),
        rollingPaperApi.getMyRollingPaper(),
      ])
        .then(
          ([
            postsRes,
            commentsRes,
            likedPostsRes,
            likedCommentsRes,
            messagesRes,
          ]) => {
            setUserStats({
              totalPosts: postsRes.data?.totalElements || 0,
              totalComments: commentsRes.data?.totalElements || 0,
              totalLikedPosts: likedPostsRes.data?.totalElements || 0,
              totalLikedComments: likedCommentsRes.data?.totalElements || 0,
              totalMessages: messagesRes.data?.length || 0,
            });
          }
        )
        .catch((err) => console.error("Failed to fetch user stats:", err));
    }
  }, [isAuthenticated, isLoading, router, user]);

  const handleNicknameChange = async () => {
    if (!nicknameInput.trim() || nicknameInput === user?.userName) {
      alert("새 닉네임을 입력하거나 현재 닉네임과 다른 닉네임을 입력해주세요.");
      return;
    }
    setIsNicknameChangeSubmitting(true);
    try {
      await updateUserName(nicknameInput);
      alert("닉네임이 성공적으로 변경되었습니다!");
      await refreshUser();
      setIsNicknameDialogOpen(false);
    } catch (error) {
      alert("닉네임 변경에 실패했습니다. 이미 사용 중인 닉네임일 수 있습니다.");
    } finally {
      setIsNicknameChangeSubmitting(false);
    }
  };

  if (isLoading || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <p>사용자 정보를 불러오는 중...</p>
      </div>
    );
  }

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.charAt(0).toUpperCase();
  };

  const fetchMyPosts = useCallback(
    async () => (await userApi.getUserPosts()).data?.content || [],
    []
  );
  const fetchMyComments = useCallback(
    async () => (await userApi.getUserComments()).data?.content || [],
    []
  );
  const fetchLikedPosts = useCallback(
    async () => (await userApi.getUserLikedPosts()).data?.content || [],
    []
  );
  const fetchLikedComments = useCallback(
    async () => (await userApi.getUserLikedComments()).data?.content || [],
    []
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />
      <main className="container mx-auto px-4 py-8">
        {/* 사용자 프로필 카드 */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
          <CardContent className="p-6 md:p-8">
            <div className="flex flex-col md:flex-row items-center md:space-x-8">
              <Avatar className="w-24 h-24 md:w-32 md:h-32 mb-4 md:mb-0">
                <AvatarImage
                  src={user.thumbnailImage || undefined}
                  alt={user.userName}
                />
                <AvatarFallback className="text-4xl">
                  {getInitials(user.userName)}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 text-center md:text-left">
                <div className="flex flex-col md:flex-row md:items-center md:space-x-4 mb-4">
                  <h2 className="text-3xl font-bold text-gray-800">
                    {user.userName}
                  </h2>
                  <Dialog
                    open={isNicknameDialogOpen}
                    onOpenChange={setIsNicknameDialogOpen}
                  >
                    <DialogTrigger asChild>
                      <Button
                        variant="outline"
                        size="sm"
                        className="mt-2 md:mt-0"
                      >
                        <Edit className="w-4 h-4 mr-2" />
                        닉네임 변경
                      </Button>
                    </DialogTrigger>
                    <DialogContent>
                      <DialogHeader>
                        <DialogTitle>닉네임 변경</DialogTitle>
                      </DialogHeader>
                      <div className="space-y-4">
                        <Input
                          value={nicknameInput}
                          onChange={(e) => setNicknameInput(e.target.value)}
                          placeholder="새 닉네임을 입력하세요"
                        />
                        <Button
                          className="w-full"
                          onClick={handleNicknameChange}
                          disabled={isNicknameChangeSubmitting}
                        >
                          {isNicknameChangeSubmitting
                            ? "변경 중..."
                            : "변경하기"}
                        </Button>
                      </div>
                    </DialogContent>
                  </Dialog>
                </div>
                <p className="text-gray-500 mb-2">{user.kakaoNickname}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 통계 카드 */}
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <Mail className="w-8 h-8 text-pink-600 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-800">
                {userStats.totalMessages}
              </p>
              <p className="text-sm text-gray-600">받은 메시지</p>
            </CardContent>
          </Card>
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <FileText className="w-8 h-8 text-blue-600 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-800">
                {userStats.totalPosts}
              </p>
              <p className="text-sm text-gray-600">작성한 글</p>
            </CardContent>
          </Card>
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <MessageCircle className="w-8 h-8 text-green-600 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-800">
                {userStats.totalComments}
              </p>
              <p className="text-sm text-gray-600">작성한 댓글</p>
            </CardContent>
          </Card>
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <Heart className="w-8 h-8 text-red-600 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-800">
                {userStats.totalLikedPosts}
              </p>
              <p className="text-sm text-gray-600">추천한 글</p>
            </CardContent>
          </Card>
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <ThumbsUp className="w-8 h-8 text-purple-600 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-800">
                {userStats.totalLikedComments}
              </p>
              <p className="text-sm text-gray-600">추천한 댓글</p>
            </CardContent>
          </Card>
        </div>

        {/* 활동 탭 */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardContent className="p-6">
            <Tabs defaultValue="my-posts" className="w-full">
              <TabsList className="grid w-full grid-cols-4 bg-gray-100">
                <TabsTrigger
                  value="my-posts"
                  className="flex items-center space-x-2"
                >
                  <FileText className="w-4 h-4" />
                  <span className="hidden sm:inline">작성한 글</span>
                  <span className="sm:hidden">글</span>
                </TabsTrigger>
                <TabsTrigger
                  value="my-comments"
                  className="flex items-center space-x-2"
                >
                  <MessageCircle className="w-4 h-4" />
                  <span className="hidden sm:inline">작성한 댓글</span>
                  <span className="sm:hidden">댓글</span>
                </TabsTrigger>
                <TabsTrigger
                  value="liked-posts"
                  className="flex items-center space-x-2"
                >
                  <Heart className="w-4 h-4" />
                  <span className="hidden sm:inline">추천한 글</span>
                  <span className="sm:hidden">추천글</span>
                </TabsTrigger>
                <TabsTrigger
                  value="liked-comments"
                  className="flex items-center space-x-2"
                >
                  <ThumbsUp className="w-4 h-4" />
                  <span className="hidden sm:inline">추천한 댓글</span>
                  <span className="sm:hidden">추천댓글</span>
                </TabsTrigger>
              </TabsList>
              <TabsContent value="my-posts">
                <ActivityTabContent fetchData={fetchMyPosts} />
              </TabsContent>
              <TabsContent value="my-comments">
                <ActivityTabContent fetchData={fetchMyComments} />
              </TabsContent>
              <TabsContent value="liked-posts">
                <ActivityTabContent fetchData={fetchLikedPosts} />
              </TabsContent>
              <TabsContent value="liked-comments">
                <ActivityTabContent fetchData={fetchLikedComments} />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
