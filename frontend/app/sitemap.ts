import { MetadataRoute } from "next";
import { boardApi, SimplePost } from "@/lib/api";

const URL = "https://grow-farm.com";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes = [
    "",
    "/board",
    "/login",
    "/signup",
    "/privacy",
    "/terms",
    "/suggest",
  ].map((route) => ({
    url: `${URL}${route}`,
    lastModified: new Date().toISOString(),
    changeFrequency: "weekly" as "weekly",
    priority: route === "" ? 1 : 0.8,
  }));

  let allPosts: SimplePost[] = [];
  let currentPage = 0;
  let totalPages = 1;

  try {
    while (currentPage < totalPages) {
      const response = await boardApi.getPosts(currentPage, 100); // Fetch 100 posts per page
      if (response.success && response.data) {
        allPosts = allPosts.concat(response.data.content);
        totalPages = response.data.totalPages;
        currentPage++;
      } else {
        break; // Exit loop on failure
      }
    }
  } catch (error) {
    console.error("Failed to fetch posts for sitemap:", error);
  }


  const postRoutes = allPosts.map((post) => ({
    url: `${URL}/board/post/${post.postId}`,
    lastModified: new Date(post.createdAt).toISOString(),
    changeFrequency: "daily" as "daily",
    priority: 0.7,
  }));

  return [...staticRoutes, ...postRoutes];
} 