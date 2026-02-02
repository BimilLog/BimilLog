'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

interface ActionResult {
  success: boolean
  message?: string
  error?: string
  data?: any
}

async function getAuthHeaders() {
  const cookieStore = await cookies()
  const cookieNames = ['jwt_access_token', 'jwt_refresh_token', 'XSRF-TOKEN', 'SCOUTER', 'temp_user_id']
  const cookieParts: string[] = []

  for (const name of cookieNames) {
    const cookie = cookieStore.get(name)
    if (cookie?.value) {
      cookieParts.push(`${name}=${cookie.value}`)
    }
  }

  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (cookieParts.length > 0) headers['Cookie'] = cookieParts.join('; ')

  const xsrfToken = cookieStore.get('XSRF-TOKEN')?.value
  if (xsrfToken) headers['X-XSRF-TOKEN'] = xsrfToken

  return headers
}

/**
 * 친구 요청 보내기
 */
export async function sendFriendRequestAction(receiverMemberId: number): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/friend/send`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ receiverMemberId }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '친구 요청에 실패했습니다.',
      }
    }

    const data = await res.json().catch(() => null)
    revalidatePath('/friends')
    return { success: true, message: '친구 요청을 보냈습니다.', data }
  } catch (error) {
    console.error('Send friend request error:', error)
    return {
      success: false,
      error: '친구 요청 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 친구 요청 취소 (보낸 요청)
 */
export async function cancelFriendRequestAction(requestId: number): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/friend/send/${requestId}`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '친구 요청 취소에 실패했습니다.',
      }
    }

    revalidatePath('/friends')
    return { success: true, message: '친구 요청을 취소했습니다.' }
  } catch (error) {
    console.error('Cancel friend request error:', error)
    return {
      success: false,
      error: '친구 요청 취소 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 친구 요청 수락
 */
export async function acceptFriendRequestAction(requestId: number): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/friend/receive/${requestId}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({}),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '친구 요청 수락에 실패했습니다.',
      }
    }

    revalidatePath('/friends')
    return { success: true, message: '친구 요청을 수락했습니다.' }
  } catch (error) {
    console.error('Accept friend request error:', error)
    return {
      success: false,
      error: '친구 요청 수락 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 친구 요청 거절
 */
export async function rejectFriendRequestAction(requestId: number): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/friend/receive/${requestId}`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '친구 요청 거절에 실패했습니다.',
      }
    }

    revalidatePath('/friends')
    return { success: true, message: '친구 요청을 거절했습니다.' }
  } catch (error) {
    console.error('Reject friend request error:', error)
    return {
      success: false,
      error: '친구 요청 거절 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 친구 삭제 (친구 관계 끊기)
 */
export async function removeFriendAction(friendshipId: number): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/friend/friendship/${friendshipId}`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '친구 삭제에 실패했습니다.',
      }
    }

    revalidatePath('/friends')
    return { success: true, message: '친구를 삭제했습니다.' }
  } catch (error) {
    console.error('Remove friend error:', error)
    return {
      success: false,
      error: '친구 삭제 중 오류가 발생했습니다.',
    }
  }
}
