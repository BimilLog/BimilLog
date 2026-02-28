'use server'

import { revalidatePath } from 'next/cache'
import { cookies } from 'next/headers'

interface ActionResult {
  success: boolean
  message?: string
  error?: string
}

interface Setting {
  messageNotification: boolean
  commentNotification: boolean
  postFeaturedNotification: boolean
  friendSendNotification: boolean
}

interface ReportData {
  reportType: 'POST' | 'COMMENT' | 'ERROR' | 'IMPROVEMENT'
  targetId?: number
  content: string
  reporterId: number | null
  reporterName: string
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
 * 사용자명 변경
 */
export async function updateUserNameAction(memberName: string): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/member/username`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ memberName }),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '사용자명 변경에 실패했습니다.',
      }
    }

    revalidatePath('/mypage')
    revalidatePath('/settings')
    return { success: true, message: '사용자명이 변경되었습니다.' }
  } catch (error) {
    console.error('Update username error:', error)
    return {
      success: false,
      error: '사용자명 변경 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 사용자 설정 업데이트
 */
export async function updateSettingsAction(settings: Setting): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/member/setting`, {
      method: 'POST',
      headers,
      body: JSON.stringify(settings),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '설정 저장에 실패했습니다.',
      }
    }

    revalidatePath('/settings')
    return { success: true, message: '설정이 저장되었습니다.' }
  } catch (error) {
    console.error('Update settings error:', error)
    return {
      success: false,
      error: '설정 저장 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 신고 제출
 */
export async function submitReportAction(report: ReportData): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/member/report`, {
      method: 'POST',
      headers,
      body: JSON.stringify(report),
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '신고 처리에 실패했습니다.',
      }
    }

    return { success: true, message: '신고가 접수되었습니다.' }
  } catch (error) {
    console.error('Submit report error:', error)
    return {
      success: false,
      error: '신고 처리 중 오류가 발생했습니다.',
    }
  }
}

/**
 * 회원 탈퇴
 */
export async function withdrawAction(): Promise<ActionResult> {
  const apiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080'
  const headers = await getAuthHeaders()

  try {
    const res = await fetch(`${apiUrl}/api/member/withdraw`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      return {
        success: false,
        error: errorData?.errorMessage || '회원 탈퇴에 실패했습니다.',
      }
    }

    // Server Action에서는 백엔드의 Set-Cookie가 브라우저에 전달되지 않으므로
    // 직접 쿠키를 만료시켜 브라우저에서 JWT 쿠키 삭제
    const cookieStore = await cookies()
    const cookieNamesToExpire = ['jwt_access_token', 'jwt_refresh_token']
    for (const name of cookieNamesToExpire) {
      cookieStore.delete(name)
    }

    return { success: true, message: '회원 탈퇴가 완료되었습니다.' }
  } catch (error) {
    console.error('Withdraw error:', error)
    return {
      success: false,
      error: '회원 탈퇴 중 오류가 발생했습니다.',
    }
  }
}
