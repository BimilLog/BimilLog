'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

interface ActionResult {
  success: boolean
  message?: string
  error?: string
}

interface BatchUpdateRequest {
  readIds: number[]
  deletedIds: number[]
}

async function getAuthHeaders() {
  const cookieStore = await cookies()
  const cookieNames = ['jwt_access_token', 'jwt_refresh_token', 'XSRF-TOKEN', 'SCOUTER']
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
 * 알림 일괄 업데이트 (읽음/삭제)
 */
export async function batchUpdateNotificationAction(data: BatchUpdateRequest): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/notification/update`, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '알림 업데이트에 실패했습니다.',
      }
    }

    return { success: true }
  } catch (error) {
    console.error('Notification batch update error:', error)
    return {
      success: false,
      error: '알림 업데이트 중 오류가 발생했습니다.',
    }
  }
}

/**
 * FCM 토큰 등록
 */
export async function registerFcmTokenAction(fcmToken: string): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/auth/fcm`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ fcmToken }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || 'FCM 토큰 등록에 실패했습니다.',
      }
    }

    return { success: true, message: 'FCM 토큰이 등록되었습니다.' }
  } catch (error) {
    console.error('FCM token registration error:', error)
    return {
      success: false,
      error: 'FCM 토큰 등록 중 오류가 발생했습니다.',
    }
  }
}
