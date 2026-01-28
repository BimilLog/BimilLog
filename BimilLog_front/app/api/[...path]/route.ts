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
const COOKIE_NAMES = ['jwt_access_token', 'jwt_refresh_token', 'XSRF-TOKEN', 'SCOUTER']

async function buildProxyResponse(response: Response): Promise<NextResponse> {
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

  const responseBody = await response.text()
  return new NextResponse(responseBody || null, {
    status: response.status,
    headers: responseHeaders,
  })
}

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

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (cookieParts.length > 0) {
    headers['Cookie'] = cookieParts.join('; ')
  }

  // CSRF 토큰 전달
  const xsrfToken = request.cookies.get('XSRF-TOKEN')?.value
  if (xsrfToken) {
    headers['X-XSRF-TOKEN'] = xsrfToken
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

  try {
    const response = await fetch(backendUrl, {
      method,
      headers,
      body,
      redirect: 'manual',
    })

    // Spring이 trailing slash로 redirect하는 경우 (301/302/307/308)
    // 수동으로 follow하여 Cookie 등 헤더가 유실되지 않도록 처리
    if ([301, 302, 307, 308].includes(response.status)) {
      const redirectLocation = response.headers.get('Location')
      if (redirectLocation) {
        const redirectUrl = new URL(redirectLocation, backendUrl)
        const redirectResponse = await fetch(redirectUrl.toString(), {
          method: [301, 302].includes(response.status) ? 'GET' : method,
          headers,
          body: [301, 302].includes(response.status) ? undefined : body,
          redirect: 'manual',
        })
        return buildProxyResponse(redirectResponse)
      }
    }

    return buildProxyResponse(response)
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
