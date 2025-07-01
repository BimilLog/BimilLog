import { useCallback } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { userApi } from "@/lib/api";
import { ActivityTabContent } from "./ActivityTabContent";

export const ActivityTabs: React.FC = () => {
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
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
      <CardContent className="p-6">
        <Tabs defaultValue="my-posts" className="w-full">
          <TabsList className="grid w-full grid-cols-4 bg-gray-100 h-12 md:h-10 p-1">
            <TabsTrigger
              value="my-posts"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">작성한 글</span>
              <span className="sm:hidden">글</span>
            </TabsTrigger>
            <TabsTrigger
              value="my-comments"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">작성한 댓글</span>
              <span className="sm:hidden">댓글</span>
            </TabsTrigger>
            <TabsTrigger
              value="liked-posts"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">추천한 글</span>
              <span className="sm:hidden">추천글</span>
            </TabsTrigger>
            <TabsTrigger
              value="liked-comments"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">추천한 댓글</span>
              <span className="sm:hidden">추천댓글</span>
            </TabsTrigger>
          </TabsList>
          <TabsContent value="my-posts">
            <ActivityTabContent fetchData={fetchMyPosts} contentType="posts" />
          </TabsContent>
          <TabsContent value="my-comments">
            <ActivityTabContent
              fetchData={fetchMyComments}
              contentType="comments"
            />
          </TabsContent>
          <TabsContent value="liked-posts">
            <ActivityTabContent
              fetchData={fetchLikedPosts}
              contentType="liked-posts"
            />
          </TabsContent>
          <TabsContent value="liked-comments">
            <ActivityTabContent
              fetchData={fetchLikedComments}
              contentType="liked-comments"
            />
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
};
