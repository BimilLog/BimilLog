'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
}

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

  return headers
}

export type ActionResult = {
  success: boolean
  message?: string
  error?: string
}

/**
 * 댓글 좋아요 토글 Server Action
 */
export async function likeCommentAction(commentId: number, postId: number): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/comment/like`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ commentId }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '좋아요 처리에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath(`/board/post/${postId}`)

    return { success: true, message: '추천 처리가 완료되었습니다.' }
  } catch (error) {
    console.error('[likeCommentAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 댓글 작성 Server Action
 */
export async function createCommentAction(data: {
  postId: number
  content: string
  parentId?: number
  password?: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/comment/write`, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '댓글 작성에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath(`/board/post/${data.postId}`)

    return { success: true, message: '댓글이 작성되었습니다.' }
  } catch (error) {
    console.error('[createCommentAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 댓글 수정 Server Action
 */
export async function updateCommentAction(data: {
  commentId: number
  postId: number
  content: string
  password?: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/comment/update`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        id: data.commentId,
        content: data.content,
        password: data.password,
      }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '댓글 수정에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath(`/board/post/${data.postId}`)

    return { success: true, message: '댓글이 수정되었습니다.' }
  } catch (error) {
    console.error('[updateCommentAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 댓글 삭제 Server Action
 */
export async function deleteCommentAction(data: {
  commentId: number
  postId: number
  password?: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/comment/delete`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        id: data.commentId,
        password: data.password,
      }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '댓글 삭제에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath(`/board/post/${data.postId}`)

    return { success: true, message: '댓글이 삭제되었습니다.' }
  } catch (error) {
    console.error('[deleteCommentAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}
