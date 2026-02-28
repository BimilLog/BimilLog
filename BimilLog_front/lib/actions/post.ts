'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
}

// 쿠키에서 인증 토큰 가져오기
async function getAuthHeaders() {
  const cookieStore = await cookies()

  // 필요한 모든 쿠키 수집
  const cookieNames = ['jwt_access_token', 'jwt_refresh_token', 'XSRF-TOKEN', 'SCOUTER']
  const cookieParts: string[] = []

  for (const name of cookieNames) {
    const cookie = cookieStore.get(name)
    if (cookie?.value) {
      cookieParts.push(`${name}=${cookie.value}`)
    }
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (cookieParts.length > 0) {
    headers['Cookie'] = cookieParts.join('; ')
  }

  // CSRF 토큰은 별도 헤더로도 전송
  const xsrfToken = cookieStore.get('XSRF-TOKEN')?.value
  if (xsrfToken) {
    headers['X-XSRF-TOKEN'] = xsrfToken
  }

  // 디버그 로그
  console.log('[getAuthHeaders] Cookies:', cookieParts.map(c => c.split('=')[0]).join(', '))

  return headers
}

export type ActionResult = {
  success: boolean
  message?: string
  error?: string
}

export type CreatePostResult = {
  success: boolean
  postId?: number
  message?: string
  error?: string
}

/**
 * 게시글 작성 Server Action
 */
export async function createPostAction(data: {
  title: string
  content: string
  password?: number
}): Promise<CreatePostResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/post`, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '게시글 작성에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    // Location 헤더에서 게시글 ID 추출
    const location = res.headers.get('location')
    let postId: number | undefined
    if (location) {
      const match = location.match(/\/post\/(\d+)$/)
      if (match) {
        postId = parseInt(match[1])
      }
    }

    revalidatePath('/board')

    return { success: true, postId, message: '게시글이 작성되었습니다.' }
  } catch (error) {
    console.error('[createPostAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 게시글 수정 Server Action
 */
export async function updatePostAction(data: {
  postId: number
  title: string
  content: string
  password?: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/post/${data.postId}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify({
        title: data.title,
        content: data.content,
        password: data.password,
      }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '게시글 수정에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath('/board')
    revalidatePath(`/board/post/${data.postId}`)

    return { success: true, message: '게시글이 수정되었습니다.' }
  } catch (error) {
    console.error('[updatePostAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 게시글 삭제 Server Action
 */
export async function deletePostAction(data: {
  postId: number
  password?: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/post/${data.postId}`, {
      method: 'DELETE',
      headers,
      body: data.password ? JSON.stringify({ password: data.password }) : undefined,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '게시글 삭제에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath('/board')

    return { success: true, message: '게시글이 삭제되었습니다.' }
  } catch (error) {
    console.error('[deletePostAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 공지사항 토글 Server Action (관리자 전용)
 */
export async function toggleNoticeAction(postId: number, notice: boolean): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/post/notice`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ postId, notice }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '공지사항 변경에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath('/board')
    revalidatePath(`/board/post/${postId}`)

    return { success: true, message: '공지사항 설정이 변경되었습니다.' }
  } catch (error) {
    console.error('[toggleNoticeAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 게시글 좋아요 토글 Server Action
 */
export async function likePostAction(postId: number): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    console.log('[likePostAction] Requesting:', `${apiUrl}/api/post/${postId}/like`)

    const res = await fetch(`${apiUrl}/api/post/${postId}/like`, {
      method: 'POST',
      headers,
      body: JSON.stringify({}),
    })

    console.log('[likePostAction] Response status:', res.status)

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      console.log('[likePostAction] Error response:', errorData)
      const errorMessage = errorData?.errorMessage || errorData?.message || '좋아요 처리에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    // 캐시 무효화
    revalidatePath('/board')
    revalidatePath(`/board/post/${postId}`)

    return { success: true, message: '추천 처리가 완료되었습니다.' }
  } catch (error) {
    console.error('[likePostAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}
