/**
 * 서버 컴포넌트용 API fetch 함수
 * SSR 시 내부 IP로 직접 호출하여 NAT Gateway 우회
 */

import { ApiResponse, PageResponse } from '@/types/common'
import { SimplePost } from '@/types/domains/post'
import { PopularPaperInfo, RollingPaperMessage, VisitPaperResult } from '@/types/domains/paper'
import { Friend, ReceivedFriendRequest, SentFriendRequest, RecommendedFriend } from '@/types/domains/friend'
import { MyPageDTO } from '@/types/domains/mypage'
import { Setting } from '@/types/domains/user'
import { cookies } from 'next/headers'

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
    isSearch: false,
  }
}

// 검색 결과 초기 데이터 조회 (SSR)
export async function getSearchInitialData(
  type: 'TITLE' | 'TITLE_CONTENT' | 'WRITER',
  query: string,
  page = 0,
  size = 30
) {
  const [searchResults, realtimePosts] = await Promise.all([
    searchPostsServer(type, query, page, size),
    getRealtimePostsServer(0, 5),
  ])

  return {
    posts: searchResults?.data || null,
    realtimePosts: realtimePosts?.data || null,
    noticePosts: null,
    currentPage: page,
    pageSize: size,
    isSearch: true,
    searchQuery: query,
    searchType: type,
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

// 인기 롤링페이퍼 조회
export async function getPopularPapersServer(page = 0, size = 10) {
  return serverFetch<ApiResponse<PageResponse<PopularPaperInfo>>>(`/api/paper/popular?page=${page}&size=${size}`)
}

/**
 * 인증 포함 서버 fetch (쿠키 전달)
 * 로그인 필요한 API를 SSR에서 호출할 때 사용
 */
async function authServerFetch<T>(endpoint: string): Promise<T | null> {
  try {
    const cookieStore = await cookies()
    const cookieHeader = cookieStore.getAll()
      .map(({ name, value }) => `${name}=${value}`)
      .join('; ')
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (cookieHeader) headers['Cookie'] = cookieHeader
    const res = await fetch(`${getServerApiUrl()}${endpoint}`, {
      headers,
      next: { revalidate: 0 },
    })
    if (!res.ok) {
      console.error(`[authServerFetch] Failed: ${endpoint}, status: ${res.status}`)
      return null
    }
    return await res.json()
  } catch (error) {
    console.error(`[authServerFetch] Error:`, error)
    return null
  }
}

// === 친구 관련 ===

export async function getMyFriendsServer(page = 0, size = 20) {
  return authServerFetch<ApiResponse<PageResponse<Friend>>>(`/api/friend/list?page=${page}&size=${size}`)
}

export async function getReceivedRequestsServer(page = 0, size = 20) {
  return authServerFetch<ApiResponse<PageResponse<ReceivedFriendRequest>>>(`/api/friend/receive?page=${page}&size=${size}`)
}

export async function getSentRequestsServer(page = 0, size = 20) {
  return authServerFetch<ApiResponse<PageResponse<SentFriendRequest>>>(`/api/friend/send?page=${page}&size=${size}`)
}

export async function getRecommendedFriendsServer(page = 0, size = 10) {
  return authServerFetch<ApiResponse<PageResponse<RecommendedFriend>>>(`/api/friend/recommend?page=${page}&size=${size}`)
}

// === 마이페이지 관련 ===

export async function getMyPageInfoServer(page = 0, size = 10) {
  return authServerFetch<ApiResponse<MyPageDTO>>(`/api/mypage/?page=${page}&size=${size}`)
}

export async function getMyRollingPaperServer() {
  return authServerFetch<ApiResponse<RollingPaperMessage[]>>(`/api/paper`)
}

// === 설정 관련 ===

export async function getUserSettingsServer() {
  return authServerFetch<ApiResponse<Setting>>(`/api/member/setting`)
}

// === 게시글 상세 ===

export async function getPostDetailServer(postId: number) {
  return authServerFetch<ApiResponse<import('@/types/domains/post').Post>>(`/api/post/${postId}`)
}

// 롤링페이퍼 방문자 조회 (인증 쿠키 포함 - 차단 체크용)
export async function getRollingPaperServer(nickname: string): Promise<ApiResponse<VisitPaperResult> | null> {
  try {
    const url = `${getServerApiUrl()}/api/paper/${encodeURIComponent(nickname)}`
    const cookieStore = await cookies()
    const cookieHeader = cookieStore.getAll()
      .map(({ name, value }) => `${name}=${value}`)
      .join('; ')

    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (cookieHeader) headers['Cookie'] = cookieHeader

    const res = await fetch(url, {
      headers,
      next: { revalidate: 0 },
    })

    if (!res.ok) {
      console.error(`[getRollingPaperServer] Failed: ${url}, status: ${res.status}`)
      return null
    }

    return await res.json()
  } catch (error) {
    console.error(`[getRollingPaperServer] Error:`, error)
    return null
  }
}
