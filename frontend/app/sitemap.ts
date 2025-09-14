import { MetadataRoute } from "next";
import { postQuery, SimplePost } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

const URL = "https://grow-farm.com";

// 게시글 나이에 따른 changeFrequency 계산
function calculateChangeFrequency(createdAt: string): "always" | "hourly" | "daily" | "weekly" | "monthly" | "yearly" | "never" {
  const now = new Date();
  const created = new Date(createdAt);
  const daysDiff = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24));

  if (daysDiff < 1) return "hourly";
  if (daysDiff < 7) return "daily";
  if (daysDiff < 30) return "weekly";
  if (daysDiff < 365) return "monthly";
  return "yearly";
}

// 게시글 인기도나 나이에 따른 우선순위 계산
function calculatePostPriority(post: SimplePost): number {
  const now = new Date();
  const created = new Date(post.createdAt);
  const daysDiff = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24));

  // 최신 게시글일수록 높은 우선순위
  if (daysDiff < 1) return 0.9;
  if (daysDiff < 7) return 0.8;
  if (daysDiff < 30) return 0.7;
  if (daysDiff < 90) return 0.6;
  return 0.5;
}

// 정적 페이지 설정
const staticPageConfig = {
  "": { changeFrequency: "daily" as const, priority: 1.0 },
  "/board": { changeFrequency: "hourly" as const, priority: 0.9 },
  "/rolling-paper": { changeFrequency: "daily" as const, priority: 0.9 },
  "/visit": { changeFrequency: "daily" as const, priority: 0.85 },
  "/suggest": { changeFrequency: "weekly" as const, priority: 0.7 },
  "/privacy": { changeFrequency: "monthly" as const, priority: 0.5 },
  "/terms": { changeFrequency: "monthly" as const, priority: 0.5 },
};

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date().toISOString();

  const staticRoutes = Object.entries(staticPageConfig).map(([route, config]) => ({
    url: `${URL}${route}`,
    lastModified: now,
    changeFrequency: config.changeFrequency,
    priority: config.priority,
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
        logger.warn(`Failed to fetch posts for page ${currentPage}`);
        break;
      }
    }
  } catch (error) {
    logger.error("Failed to fetch posts for sitemap:", error);
    return staticRoutes;
  }

  // 게시글별로 동적으로 changeFrequency와 priority 계산
  const postRoutes = allPosts.map((post) => ({
    url: `${URL}/board/post/${post.id}`,
    lastModified: new Date(post.createdAt).toISOString(),
    changeFrequency: calculateChangeFrequency(post.createdAt),
    priority: calculatePostPriority(post),
  }));

  return [...staticRoutes, ...postRoutes];
}

// 추가 사이트맵 생성 함수들 (Next.js가 자동으로 인식)
export async function sitemapPosts(): Promise<MetadataRoute.Sitemap> {
  // 게시글 전용 사이트맵
  if (process.env.NODE_ENV === 'development' || !process.env.NEXT_PUBLIC_API_URL) {
    return [];
  }

  let allPosts: SimplePost[] = [];
  let currentPage = 0;
  let totalPages = 1;

  try {
    while (currentPage < totalPages && currentPage < 50) { // 최대 50페이지까지만
      const response = await postQuery.getAll(currentPage, 100);
      if (response.success && response.data) {
        allPosts = allPosts.concat(response.data.content);
        totalPages = response.data.totalPages;
        currentPage++;
      } else {
        break;
      }
    }
  } catch (error) {
    logger.error("Failed to fetch posts for posts sitemap:", error);
    return [];
  }

  return allPosts.map((post) => ({
    url: `${URL}/board/post/${post.id}`,
    lastModified: new Date(post.createdAt).toISOString(),
    changeFrequency: calculateChangeFrequency(post.createdAt),
    priority: calculatePostPriority(post),
  }));
}

export async function sitemapPapers(): Promise<MetadataRoute.Sitemap> {
  // 롤링페이퍼 페이지 사이트맵 (향후 구현 예정)
  // 현재는 빈 배열 반환
  return [];
} 