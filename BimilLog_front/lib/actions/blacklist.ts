'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
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

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (cookieParts.length > 0) {
    headers['Cookie'] = cookieParts.join('; ')
  }

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
 * 블랙리스트에 사용자 추가 Server Action
 */
export async function addToBlacklistAction(memberName: string): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/member/blacklist`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ memberName }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '블랙리스트 추가에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath('/blacklist')

    return { success: true, message: '블랙리스트에 추가되었습니다.' }
  } catch (error) {
    console.error('[addToBlacklistAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 블랙리스트에서 사용자 삭제 Server Action
 */
export async function removeFromBlacklistAction(id: number): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/member/blacklist/${id}?page=0&size=20&sort=createdAt,desc`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '블랙리스트 삭제에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    revalidatePath('/blacklist')

    return { success: true, message: '블랙리스트에서 삭제되었습니다.' }
  } catch (error) {
    console.error('[removeFromBlacklistAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}
