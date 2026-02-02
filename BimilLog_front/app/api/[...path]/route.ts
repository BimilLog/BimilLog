import { NextRequest, NextResponse } from 'next/server'

/**
 * API 프록시 라우트 핸들러
 * 브라우저 → Next.js 서버 → 백엔드 (내부 통신)
 *
 * 모든 /api/* 요청을 백엔드로 프록시하여 NAT Gateway 비용 절감
 */

const getInternalApiUrl = () => {
  return process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
}

// 전달할 쿠키 이름들
const COOKIE_NAMES = ['jwt_access_token', 'jwt_refresh_token', 'XSRF-TOKEN', 'SCOUTER', 'temp_user_id']

async function proxyRequest(request: NextRequest, method: string) {
  const url = new URL(request.url)
  const path = url.pathname // /api/...
  const search = url.search // ?page=0&size=10

  const backendUrl = `${getInternalApiUrl()}${path}${search}`

  // 쿠키 전달
  const cookieParts: string[] = []
  for (const name of COOKIE_NAMES) {
    const cookie = request.cookies.get(name)
    if (cookie?.value) {
      cookieParts.push(`${name}=${cookie.value}`)
    }
  }

  const headers: Record<string, string> = {}

  if (cookieParts.length > 0) {
    headers['Cookie'] = cookieParts.join('; ')
  }

  // CSRF 토큰 전달
  const xsrfToken = request.cookies.get('XSRF-TOKEN')?.value
  if (xsrfToken) {
    headers['X-XSRF-TOKEN'] = xsrfToken
  }

  // 클라이언트 IP 전달
  const forwardedFor = request.headers.get('x-forwarded-for')
  if (forwardedFor) {
    headers['X-Forwarded-For'] = forwardedFor
  } else {
    const realIp = request.headers.get('x-real-ip')
    if (realIp) {
      headers['X-Forwarded-For'] = realIp
    }
  }

  // 요청 본문 처리
  let body: string | undefined
  if (method !== 'GET' && method !== 'HEAD') {
    try {
      const text = await request.text()
      if (text) {
        body = text
      }
    } catch {
      // 빈 본문
    }
  }

  // body가 있을 때만 Content-Type 설정
  if (body) {
    headers['Content-Type'] = 'application/json'
  }

  try {
    const response = await fetch(backendUrl, {
      method,
      headers,
      body,
    })

    // 응답 헤더 복사 (Set-Cookie 포함)
    const responseHeaders = new Headers()

    // Set-Cookie 헤더 전달
    const setCookieHeaders = response.headers.getSetCookie?.() || []
    for (const cookie of setCookieHeaders) {
      responseHeaders.append('Set-Cookie', cookie)
    }

    // Content-Type 전달
    const contentType = response.headers.get('Content-Type')
    if (contentType) {
      responseHeaders.set('Content-Type', contentType)
    }

    // Location 헤더 전달 (201 Created 등)
    const location = response.headers.get('Location')
    if (location) {
      responseHeaders.set('Location', location)
    }

    // Content-Length 헤더 전달 (빈 응답 감지용)
    const contentLength = response.headers.get('Content-Length')
    if (contentLength) {
      responseHeaders.set('Content-Length', contentLength)
    }

    // 응답 본문 처리
    const responseBody = await response.text()

    return new NextResponse(responseBody || null, {
      status: response.status,
      headers: responseHeaders,
    })
  } catch (error) {
    console.error('[API Proxy] Error:', error)
    return NextResponse.json(
      { error: 'Internal proxy error' },
      { status: 502 }
    )
  }
}

export async function GET(request: NextRequest) {
  return proxyRequest(request, 'GET')
}

export async function POST(request: NextRequest) {
  return proxyRequest(request, 'POST')
}

export async function PUT(request: NextRequest) {
  return proxyRequest(request, 'PUT')
}

export async function DELETE(request: NextRequest) {
  return proxyRequest(request, 'DELETE')
}

export async function PATCH(request: NextRequest) {
  return proxyRequest(request, 'PATCH')
}
