'use server'

import { revalidatePath, revalidateTag } from 'next/cache'
import { cookies } from 'next/headers'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
}

// 쿠키에서 인증 토큰 가져오기
async function getAuthHeaders() {
  const cookieStore = await cookies()
  const accessToken = cookieStore.get('jwt_access_token')?.value
  const xsrfToken = cookieStore.get('XSRF-TOKEN')?.value

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (accessToken) {
    headers['Cookie'] = `jwt_access_token=${accessToken}`
  }

  if (xsrfToken) {
    headers['X-XSRF-TOKEN'] = xsrfToken
    // 쿠키에도 XSRF-TOKEN 추가
    if (headers['Cookie']) {
      headers['Cookie'] += `; XSRF-TOKEN=${xsrfToken}`
    } else {
      headers['Cookie'] = `XSRF-TOKEN=${xsrfToken}`
    }
  }

  return headers
}

export type ActionResult = {
  success: boolean
  message?: string
  error?: string
}

/**
 * 게시글 좋아요 토글 Server Action
 */
export async function likePostAction(postId: number): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/post/${postId}/like`, {
      method: 'POST',
      headers,
      body: JSON.stringify({}),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
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
