'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'
import { DecoType } from '@/types/domains/paper'

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
 * 롤링페이퍼 메시지 작성 Server Action
 */
export async function createMessageAction(data: {
  userName: string
  decoType: DecoType
  anonymity: string
  content: string
  x: number
  y: number
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/paper/${encodeURIComponent(data.userName)}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        decoType: data.decoType,
        anonymity: data.anonymity,
        content: data.content,
        x: data.x,
        y: data.y,
      }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '메시지 작성에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    // 해당 유저의 롤링페이퍼 캐시 무효화
    revalidatePath(`/rolling-paper/${encodeURIComponent(data.userName)}`)

    return { success: true, message: '메시지가 작성되었습니다.' }
  } catch (error) {
    console.error('[createMessageAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}

/**
 * 롤링페이퍼 메시지 삭제 Server Action
 */
export async function deleteMessageAction(data: {
  messageId: number
  userName?: string // 캐시 무효화용
}): Promise<ActionResult> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/paper/delete`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ id: data.messageId }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '메시지 삭제에 실패했습니다.'
      return { success: false, error: errorMessage }
    }

    // 내 롤링페이퍼 캐시 무효화
    revalidatePath('/rolling-paper')
    if (data.userName) {
      revalidatePath(`/rolling-paper/${encodeURIComponent(data.userName)}`)
    }

    return { success: true, message: '메시지가 삭제되었습니다.' }
  } catch (error) {
    console.error('[deleteMessageAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.' }
  }
}
