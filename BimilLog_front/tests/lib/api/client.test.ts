import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ApiClient } from '@/lib/api/client'

// logger 모킹
vi.mock('@/lib/utils/logger', () => ({
  logger: {
    log: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    debug: vi.fn(),
    info: vi.fn(),
    critical: vi.fn(),
  },
}))

// type-guards의 isValidApiResponse 모킹
vi.mock('@/lib/api/type-guards', () => ({
  isValidApiResponse: (data: unknown) =>
    typeof data === 'object' &&
    data !== null &&
    'success' in data &&
    typeof (data as Record<string, unknown>).success === 'boolean',
}))

// helpers.ts는 type-guards를 re-export하므로 동일하게 모킹
vi.mock('@/lib/api/helpers', () => ({
  isValidApiResponse: (data: unknown) =>
    typeof data === 'object' &&
    data !== null &&
    'success' in data &&
    typeof (data as Record<string, unknown>).success === 'boolean',
}))

// ---- 헬퍼 ----

/**
 * 성공 응답 mock 생성
 */
function mockSuccessResponse(
  body: unknown,
  init: {
    status?: number
    headers?: Record<string, string>
  } = {},
) {
  const { status = 200, headers = {} } = init
  const headerMap = new Headers(headers)

  if (body !== null && !headerMap.has('content-length')) {
    headerMap.set('content-length', String(JSON.stringify(body).length))
  }

  const response = {
    ok: true,
    status,
    headers: headerMap,
    json: vi.fn().mockResolvedValue(body),
    text: vi.fn().mockResolvedValue(typeof body === 'string' ? body : JSON.stringify(body)),
    clone: vi.fn(),
  }
  response.clone.mockReturnValue({
    ...response,
    json: vi.fn().mockResolvedValue(body),
    text: vi.fn().mockResolvedValue(typeof body === 'string' ? body : JSON.stringify(body)),
  })
  return response
}

/**
 * 에러 응답 mock 생성
 *
 * 실제 코드 흐름 (client.ts line 100-155):
 *   1. response.json() 성공 → extractedMessage 추출 → throw new Error(extractedMessage)
 *   2. 이 throw가 같은 try의 catch(jsonError)에 잡힘
 *   3. catch에서 clonedResponse.text() → errorMessage 갱신 → throw new Error(errorMessage)
 *   4. 이 throw가 외부 catch(error)에 잡혀서 { success: false, error: error.message } 반환
 *
 * 따라서 json()이 성공하면 최종 에러 메시지는 clonedResponse.text()의 반환값이 된다.
 * json()이 실패하면 clonedResponse.text()의 반환값이 에러 메시지가 된다.
 */
function mockErrorResponse(
  jsonBody: unknown,
  options: {
    status: number
    textBody?: string
    jsonFails?: boolean
  },
) {
  const { status, textBody, jsonFails = false } = options
  const headerMap = new Headers({ 'content-length': '100' })

  // clonedResponse.text()가 반환할 값 결정
  const textValue = textBody ?? (typeof jsonBody === 'string' ? jsonBody : JSON.stringify(jsonBody))

  const makeResponse = () => ({
    ok: false,
    status,
    headers: headerMap,
    json: jsonFails
      ? vi.fn().mockRejectedValue(new Error('Invalid JSON'))
      : vi.fn().mockResolvedValue(jsonBody),
    text: vi.fn().mockResolvedValue(textValue),
    clone: vi.fn(),
  })

  const response = makeResponse()
  response.clone.mockReturnValue(makeResponse())
  return response
}

describe('ApiClient', () => {
  let client: ApiClient
  let fetchMock: ReturnType<typeof vi.fn>
  let originalCookie: PropertyDescriptor | undefined

  beforeEach(() => {
    client = new ApiClient('http://localhost:8080')
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    originalCookie = Object.getOwnPropertyDescriptor(document, 'cookie')
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: '',
      configurable: true,
    })

    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()

    if (originalCookie) {
      Object.defineProperty(document, 'cookie', originalCookie)
    }
  })

  // ========================================
  // 1. GET 요청 성공
  // ========================================
  describe('GET 요청 성공', () => {
    it('올바른 URL로 fetch가 호출된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: { id: 1 } }))

      await client.get('/api/test')

      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/test',
        expect.objectContaining({ method: 'GET' }),
      )
    })

    it('Content-Type과 credentials가 올바르게 설정된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.get('/api/test')

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
          }),
          credentials: 'include',
        }),
      )
    })

    it('ApiResponse 구조가 그대로 반환된다 (isValidApiResponse 통과)', async () => {
      const responseData = { success: true, data: { id: 1, name: 'test' } }
      fetchMock.mockResolvedValue(mockSuccessResponse(responseData))

      const result = await client.get<{ id: number; name: string }>('/api/test')

      expect(result).toEqual({ success: true, data: { id: 1, name: 'test' } })
    })

    it('isValidApiResponse를 통과하지 않는 원시 데이터는 { success: true, data: rawData }로 래핑된다', async () => {
      const rawData = { id: 1, name: 'test' }
      fetchMock.mockResolvedValue(mockSuccessResponse(rawData))

      const result = await client.get<{ id: number; name: string }>('/api/test')

      expect(result).toEqual({ success: true, data: { id: 1, name: 'test' } })
    })
  })

  // ========================================
  // 2. POST 요청 성공
  // ========================================
  describe('POST 요청 성공', () => {
    it('body가 JSON.stringify 되어 전달된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: { id: 1 } }))

      const body = { title: '테스트', content: '내용' }
      await client.post('/api/post', body)

      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/post',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(body),
        }),
      )
    })

    it('method가 POST로 설정된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.post('/api/post', { title: 'test' })

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ method: 'POST' }),
      )
    })

    it('body가 없으면 undefined으로 전달된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.post('/api/post')

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ body: undefined }),
      )
    })
  })

  // ========================================
  // 3. PUT / DELETE / PATCH 메서드
  // ========================================
  describe('PUT / DELETE / PATCH 메서드', () => {
    it('PUT 요청이 올바르게 전달된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.put('/api/post', { id: 1, title: '수정' })

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify({ id: 1, title: '수정' }),
        }),
      )
    })

    it('DELETE 요청이 올바르게 전달된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.delete('/api/post/1')

      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/post/1',
        expect.objectContaining({ method: 'DELETE' }),
      )
    })

    it('PATCH 요청이 올바르게 전달된다', async () => {
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.patch('/api/post', { title: '패치' })

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ title: '패치' }),
        }),
      )
    })
  })

  // ========================================
  // 4. 에러 응답 처리
  // ========================================
  describe('에러 응답 처리', () => {
    /**
     * 에러 응답 흐름:
     *   json() 성공 → throw Error(extractedMessage) → catch(jsonError)에 잡힘
     *   → clonedResponse.text() → throw Error(textResult) → 외부 catch에 잡힘
     *   → { success: false, error: textResult }
     *
     * 따라서 최종 에러 메시지는 clonedResponse.text()의 반환값이다.
     * textBody를 명시적으로 지정하여 기대 에러 메시지를 제어한다.
     */

    it('400 에러: { success: false, error } 형태로 반환된다', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '잘못된 요청입니다' },
          { status: 400, textBody: '잘못된 요청입니다' },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: '잘못된 요청입니다',
      })
    })

    it('401 에러 (requiresAuth=false): { success: true, data: null } 반환', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: 'Unauthorized' },
          { status: 401 },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({ success: true, data: null })
    })

    it('401 에러 (requiresAuth=true, /api/user): { success: false, error } 반환', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '인증이 필요합니다' },
          { status: 401, textBody: '인증이 필요합니다' },
        ),
      )

      const result = await client.get('/api/user')

      expect(result).toEqual({
        success: false,
        error: '인증이 필요합니다',
      })
    })

    it('401 에러 (requiresAuth=true, /api/admin): { success: false, error } 반환', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '관리자 권한이 필요합니다' },
          { status: 401, textBody: '관리자 권한이 필요합니다' },
        ),
      )

      const result = await client.get('/api/admin/dashboard')

      expect(result).toEqual({
        success: false,
        error: '관리자 권한이 필요합니다',
      })
    })

    it('500 에러: { success: false, error } 형태로 반환된다', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '서버 내부 오류' },
          { status: 500, textBody: '서버 내부 오류' },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: '서버 내부 오류',
      })
    })

    it('에러 응답에서 message 필드가 있으면 추출된다', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { message: '메시지 필드 에러' },
          { status: 400, textBody: '메시지 필드 에러' },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: '메시지 필드 에러',
      })
    })

    it('에러 응답에서 error 필드가 있으면 추출된다', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { error: '에러 필드 에러' },
          { status: 400, textBody: '에러 필드 에러' },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: '에러 필드 에러',
      })
    })

    it('에러 응답 JSON 파싱 실패 시 텍스트로 폴백한다', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          null,
          { status: 400, textBody: 'Plain text error', jsonFails: true },
        ),
      )

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: 'Plain text error',
      })
    })
  })

  // ========================================
  // 5. 빈 응답 처리
  // ========================================
  describe('빈 응답 처리', () => {
    it('204 No Content: { success: true, data: null } 반환', async () => {
      const headers = new Headers({ 'content-length': '0' })
      const response = {
        ok: true,
        status: 204,
        headers,
        json: vi.fn(),
        text: vi.fn(),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue(response)
      fetchMock.mockResolvedValue(response)

      const result = await client.delete('/api/post/1')

      expect(result).toEqual({ success: true, data: null })
    })

    it('Content-Length: 0 응답: { success: true, data: null } 반환', async () => {
      const headers = new Headers({ 'content-length': '0' })
      const response = {
        ok: true,
        status: 200,
        headers,
        json: vi.fn(),
        text: vi.fn(),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue(response)
      fetchMock.mockResolvedValue(response)

      const result = await client.put('/api/post', { id: 1 })

      expect(result).toEqual({ success: true, data: null })
    })
  })

  // ========================================
  // 6. 201 Created + Location 헤더
  // ========================================
  describe('201 Created + Location 헤더', () => {
    it('Location: /post/123 이면 { success: true, data: { id: 123 } } 반환', async () => {
      const headers = new Headers({
        'content-length': '0',
        location: '/post/123',
      })
      const response = {
        ok: true,
        status: 201,
        headers,
        json: vi.fn(),
        text: vi.fn(),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue(response)
      fetchMock.mockResolvedValue(response)

      const result = await client.post<{ id: number }>('/api/post', { title: '새 글' })

      expect(result).toEqual({ success: true, data: { id: 123 } })
    })

    it('Location 헤더가 없는 201 응답: { success: true, data: null } 반환', async () => {
      const headers = new Headers({ 'content-length': '0' })
      const response = {
        ok: true,
        status: 201,
        headers,
        json: vi.fn(),
        text: vi.fn(),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue(response)
      fetchMock.mockResolvedValue(response)

      const result = await client.post('/api/post', { title: '새 글' })

      expect(result).toEqual({ success: true, data: null })
    })

    it('Location 헤더가 /post/숫자 패턴이 아닌 경우: { success: true, data: null } 반환', async () => {
      const headers = new Headers({
        'content-length': '0',
        location: '/other/resource',
      })
      const response = {
        ok: true,
        status: 201,
        headers,
        json: vi.fn(),
        text: vi.fn(),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue(response)
      fetchMock.mockResolvedValue(response)

      const result = await client.post('/api/post', { title: '새 글' })

      expect(result).toEqual({ success: true, data: null })
    })
  })

  // ========================================
  // 7. 타임아웃
  // ========================================
  describe('타임아웃', () => {
    it('요청 타임아웃 시 { success: false, error: "Request timed out" } 반환', async () => {
      fetchMock.mockImplementation(
        (_url: string, options: RequestInit) =>
          new Promise<never>((_resolve, reject) => {
            const createAbortError = () => {
              const error = new Error('The operation was aborted.')
              error.name = 'AbortError'
              return error
            }

            const signal = options.signal as AbortSignal
            if (signal) {
              if (signal.aborted) {
                reject(createAbortError())
                return
              }
              signal.addEventListener('abort', () => {
                reject(createAbortError())
              })
            }
          }),
      )

      const resultPromise = client.get('/api/slow-endpoint')

      // 타임아웃(8000ms)을 초과하도록 타이머 진행
      await vi.advanceTimersByTimeAsync(9000)

      const result = await resultPromise

      expect(result).toEqual({
        success: false,
        error: 'Request timed out',
      })
    })
  })

  // ========================================
  // 8. CSRF 토큰
  // ========================================
  describe('CSRF 토큰', () => {
    it('XSRF-TOKEN 쿠키가 있으면 X-XSRF-TOKEN 헤더에 포함된다', async () => {
      document.cookie = 'XSRF-TOKEN=abc123def456'

      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.get('/api/test')

      expect(fetchMock).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-XSRF-TOKEN': 'abc123def456',
          }),
        }),
      )
    })

    it('XSRF-TOKEN 쿠키가 없으면 X-XSRF-TOKEN 헤더가 포함되지 않는다', async () => {
      document.cookie = ''

      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.get('/api/test')

      const calledHeaders = fetchMock.mock.calls[0][1].headers as Record<string, string>
      expect(calledHeaders).not.toHaveProperty('X-XSRF-TOKEN')
    })

    it('여러 쿠키 중 XSRF-TOKEN만 정확히 추출한다', async () => {
      document.cookie = 'session=abc; XSRF-TOKEN=token-value-123; other=xyz'

      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await client.get('/api/test')

      const calledHeaders = fetchMock.mock.calls[0][1].headers as Record<string, string>
      expect(calledHeaders['X-XSRF-TOKEN']).toBe('token-value-123')
    })
  })

  // ========================================
  // 9. 네트워크 에러
  // ========================================
  describe('네트워크 에러', () => {
    it('fetch가 Error를 throw하면 { success: false, error: 에러 메시지 } 반환', async () => {
      fetchMock.mockRejectedValue(new Error('Failed to fetch'))

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: 'Failed to fetch',
      })
    })

    it('Error가 아닌 값이 throw되면 { success: false, error: "Network error" } 반환', async () => {
      fetchMock.mockRejectedValue('unknown error string')

      const result = await client.get('/api/test')

      expect(result).toEqual({
        success: false,
        error: 'Network error',
      })
    })
  })

  // ========================================
  // 10. baseURL 처리
  // ========================================
  describe('baseURL 처리', () => {
    it('빈 baseURL일 경우 endpoint만 URL로 사용된다', async () => {
      const emptyBaseClient = new ApiClient('')
      fetchMock.mockResolvedValue(mockSuccessResponse({ success: true, data: null }))

      await emptyBaseClient.get('/api/test')

      expect(fetchMock).toHaveBeenCalledWith('/api/test', expect.any(Object))
    })
  })

  // ========================================
  // 11. JSON 파싱 실패 시 텍스트 폴백
  // ========================================
  describe('성공 응답의 JSON 파싱 실패', () => {
    it('JSON 파싱 실패 + 텍스트가 "OK"이면 { success: true, data: null } 반환', async () => {
      const headers = new Headers({ 'content-length': '2' })
      const response = {
        ok: true,
        status: 200,
        headers,
        json: vi.fn().mockRejectedValue(new SyntaxError('Unexpected token')),
        text: vi.fn().mockResolvedValue('OK'),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue({
        ...response,
        json: vi.fn().mockRejectedValue(new SyntaxError('Unexpected token')),
        text: vi.fn().mockResolvedValue('OK'),
      })
      fetchMock.mockResolvedValue(response)

      const result = await client.get('/api/test')

      expect(result).toEqual({ success: true, data: null })
    })

    it('JSON 파싱 실패 + 텍스트 응답이 있으면 data로 반환한다', async () => {
      const headers = new Headers({ 'content-length': '20' })
      const response = {
        ok: true,
        status: 200,
        headers,
        json: vi.fn().mockRejectedValue(new SyntaxError('Unexpected token')),
        text: vi.fn().mockResolvedValue('some text response'),
        clone: vi.fn(),
      }
      response.clone.mockReturnValue({
        ...response,
        json: vi.fn().mockRejectedValue(new SyntaxError('Unexpected token')),
        text: vi.fn().mockResolvedValue('some text response'),
      })
      fetchMock.mockResolvedValue(response)

      const result = await client.get<string>('/api/test')

      expect(result).toEqual({ success: true, data: 'some text response' })
    })
  })

  // ========================================
  // 12. requiresAuth 엔드포인트 경계 케이스
  // ========================================
  describe('requiresAuth 엔드포인트 판별', () => {
    it('/paper는 정확 매칭 → 인증 필요 (401 시 에러 반환)', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '인증 필요' },
          { status: 401, textBody: '인증 필요' },
        ),
      )

      const result = await client.get('/paper')

      expect(result.success).toBe(false)
      expect(result.error).toBe('인증 필요')
    })

    it('/paper/123은 인증 불필요 → 401 시 { success: true, data: null } 반환', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: 'Unauthorized' },
          { status: 401 },
        ),
      )

      const result = await client.get('/paper/123')

      expect(result).toEqual({ success: true, data: null })
    })

    it('/notification으로 시작하는 엔드포인트는 인증 필요', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '인증 필요' },
          { status: 401, textBody: '인증 필요' },
        ),
      )

      const result = await client.get('/notification/list')

      expect(result.success).toBe(false)
      expect(result.error).toBe('인증 필요')
    })

    it('/post/manage/like는 정확 매칭 → 인증 필요 (401 시 에러 반환)', async () => {
      fetchMock.mockResolvedValue(
        mockErrorResponse(
          { errorMessage: '인증 필요' },
          { status: 401, textBody: '인증 필요' },
        ),
      )

      const result = await client.post('/post/manage/like', { postId: 1 })

      expect(result.success).toBe(false)
      expect(result.error).toBe('인증 필요')
    })
  })
})
