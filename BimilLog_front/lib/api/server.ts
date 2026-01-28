/**
 * 서버 컴포넌트용 API fetch 함수
 * SSR 시 내부 IP로 직접 호출하여 NAT Gateway 우회
 */

import { ApiResponse, PageResponse } from '@/types/common'
import { SimplePost } from '@/types/domains/post'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
}

async function serverFetch<T>(endpoint: string): Promise<T | null> {
  try {
    const url = `${getServerApiUrl()}${endpoint}`
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      next: { revalidate: 60 }, // 60초 캐시
    })

    if (!res.ok) {
      console.error(`[serverFetch] Failed: ${url}, status: ${res.status}`)
      return null
    }

    return await res.json()
  } catch (error) {
    console.error(`[serverFetch] Error:`, error)
    return null
  }
}

// 게시글 목록 조회
export async function getPostsServer(page = 0, size = 30) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(`/api/post?page=${page}&size=${size}`)
}

// 실시간 인기글 조회
export async function getRealtimePostsServer(page = 0, size = 5) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(`/api/post/realtime?page=${page}&size=${size}`)
}

// 주간 인기글 조회
export async function getWeeklyPostsServer(page = 0, size = 10) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(`/api/post/weekly?page=${page}&size=${size}`)
}

// 레전드 글 조회
export async function getLegendPostsServer(page = 0, size = 10) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(`/api/post/legend?page=${page}&size=${size}`)
}

// 공지사항 조회
export async function getNoticePostsServer(page = 0, size = 10) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(`/api/post/notice?page=${page}&size=${size}`)
}

// 게시판 초기 데이터 한 번에 조회 (페이지 파라미터 지원)
export async function getBoardInitialData(page = 0, size = 30) {
  const [posts, realtimePosts, noticePosts] = await Promise.all([
    getPostsServer(page, size),
    getRealtimePostsServer(0, 5),
    getNoticePostsServer(0, 10),
  ])

  return {
    posts: posts?.data || null,
    realtimePosts: realtimePosts?.data || null,
    noticePosts: noticePosts?.data || null,
    currentPage: page,
    pageSize: size,
  }
}

// 검색 결과 조회 (서버)
export async function searchPostsServer(
  type: 'TITLE' | 'TITLE_CONTENT' | 'WRITER',
  query: string,
  page = 0,
  size = 30
) {
  return serverFetch<ApiResponse<PageResponse<SimplePost>>>(
    `/api/post/search?type=${type}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`
  )
}
