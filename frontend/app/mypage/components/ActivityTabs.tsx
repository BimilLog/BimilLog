import { useCallback } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { userApi } from "@/lib/api";
import { ActivityTabContent } from "./ActivityTabContent";

// 페이지네이션 상수
const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 10;

export const ActivityTabs: React.FC = () => {
  const fetchMyPosts = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userApi.getUserPosts(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        console.error("Failed to fetch user posts:", error);
        throw error;
      }
    },
    []
  );

  const fetchMyComments = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userApi.getUserComments(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        console.error("Failed to fetch user comments:", error);
        throw error;
      }
    },
    []
  );

  const fetchLikedPosts = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userApi.getUserLikedPosts(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        console.error("Failed to fetch liked posts:", error);
        throw error;
      }
    },
    []
  );

  const fetchLikedComments = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userApi.getUserLikedComments(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        console.error("Failed to fetch liked comments:", error);
        throw error;
      }
    },
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
