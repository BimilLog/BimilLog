'use server'

import { cookies } from 'next/headers'
import type { PageResponse } from '@/types/common'
import type { BlacklistDTO } from '@/types/domains/blacklist'

const getServerApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
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

/**
 * 라운드 15 F-15-BUG-1/F-15-BUG-15: 백엔드 errorMessage 가 '이미 차단' / 'duplicate' / 'unique' 키워드를
 * 포함할 경우 클라이언트에서 친화적 카피로 분기할 수 있도록 코드 형태로도 노출.
 */
export type BlacklistErrorCode = 'DUPLICATE' | 'UNKNOWN'

export type ActionResult<T = void> = T extends void
  ? { success: true; message?: string } | { success: false; error: string; code?: BlacklistErrorCode }
  : { success: true; message?: string; data: T } | { success: false; error: string; code?: BlacklistErrorCode }

function detectBlacklistErrorCode(message: string | undefined): BlacklistErrorCode {
  if (!message) return 'UNKNOWN'
  const lower = message.toLowerCase()
  if (
    message.includes('이미') ||
    lower.includes('duplicate') ||
    lower.includes('unique') ||
    lower.includes('already')
  ) {
    return 'DUPLICATE'
  }
  return 'UNKNOWN'
}

/**
 * 블랙리스트에 사용자 추가 Server Action
 *
 * 라운드 15 F-15-BUG-1: 백엔드 응답 본문이 비어있어 추가 직후 id 를 모름 →
 * 클라이언트는 invalidate 후 목록에서 memberName 으로 id 를 찾아 활용.
 * revalidatePath 는 클라이언트가 이미 옵티미스틱 + invalidate 로 처리하므로 제거.
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
      return { success: false, error: errorMessage, code: detectBlacklistErrorCode(errorMessage) }
    }

    return { success: true, message: '블랙리스트에 추가했어요.' }
  } catch (error) {
    console.error('[addToBlacklistAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.', code: 'UNKNOWN' }
  }
}

/**
 * 블랙리스트에서 사용자 삭제 Server Action
 *
 * 라운드 15 F-15-BUG-1:
 * - DELETE URL 의 page/size/sort 하드코딩 제거 → URL SSOT (라운드 6 B-002).
 * - 백엔드가 삭제 후 반환하는 Page<BlacklistDTO> 응답 본문을 그대로 클라이언트로 전달 →
 *   클라이언트가 setQueryData 로 직접 캐시 갱신, invalidate 비용 0.
 * - revalidatePath 제거 (옵티미스틱 + 응답 활용으로 불필요).
 */
export async function removeFromBlacklistAction(
  id: number,
): Promise<ActionResult<PageResponse<BlacklistDTO>>> {
  try {
    const apiUrl = getServerApiUrl()
    const headers = await getAuthHeaders()

    const res = await fetch(`${apiUrl}/api/member/blacklist/${id}`, {
      method: 'DELETE',
      headers,
    })

    if (!res.ok) {
      const errorData = await res.json().catch(() => null)
      const errorMessage = errorData?.errorMessage || errorData?.message || '블랙리스트 삭제에 실패했습니다.'
      return { success: false, error: errorMessage, code: detectBlacklistErrorCode(errorMessage) }
    }

    // 백엔드 응답 본문이 Page<BlacklistDTO> 형태로 첫 페이지 데이터를 돌려줌
    const body = await res.json().catch(() => null) as PageResponse<BlacklistDTO> | null

    return {
      success: true,
      message: '차단을 풀었어요.',
      data: body ?? {
        content: [],
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        number: 0,
        size: 20,
        numberOfElements: 0,
        empty: true,
      },
    }
  } catch (error) {
    console.error('[removeFromBlacklistAction] Error:', error)
    return { success: false, error: '네트워크 오류가 발생했습니다.', code: 'UNKNOWN' }
  }
}
