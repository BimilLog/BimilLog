import { MetadataRoute } from "next";
import { postQuery, SimplePost } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

const URL = "https://grow-farm.com";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes = [
    "",
    "/board",
    "/rolling-paper",
    "/visit",
    "/privacy",
    "/terms",
    "/suggest",
  ].map((route) => ({
    url: `${URL}${route}`,
    lastModified: new Date().toISOString(),
    changeFrequency: "weekly" as "weekly",
    priority: route === "" ? 1 : 
             route === "/rolling-paper" ? 0.9 :
             route === "/visit" ? 0.9 :
             route === "/board" ? 0.8 : 0.7,
  }));

  // 개발 환경이나 빌드 시에 API 서버가 없는 경우 정적 페이지만 반환
  if (process.env.NODE_ENV === 'development' || !process.env.NEXT_PUBLIC_API_URL) {
    
    return staticRoutes;
  }

  let allPosts: SimplePost[] = [];
  let currentPage = 0;
  let totalPages = 1;

  try {
    while (currentPage < totalPages) {
      const response = await postQuery.getAll(currentPage, 100);
      if (response.success && response.data) {
        allPosts = allPosts.concat(response.data.content);
        totalPages = response.data.totalPages;
        currentPage++;
      } else {
        console.warn(`Failed to fetch posts for page ${currentPage}`);
        break;
      }
    }
    

  } catch (error) {
    logger.error("Failed to fetch posts for sitemap:", error);

    return staticRoutes;
  }

  const postRoutes = allPosts.map((post) => ({
    url: `${URL}/board/post/${post.id}`,
    lastModified: new Date(post.createdAt).toISOString(),
    changeFrequency: "daily" as "daily",
    priority: 0.7,
  }));

  return [...staticRoutes, ...postRoutes];
} 