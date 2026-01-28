import { NextRequest, NextResponse } from 'next/server';

export function middleware(request: NextRequest) {
    const nonce = Buffer.from(crypto.randomUUID()).toString('base64');
    const isDev = process.env.NODE_ENV === 'development';

    const cspDirectives = [
        "default-src 'self'",

        // script-src: nonce 기반 + strict-dynamic
        // 'unsafe-inline'은 nonce 미지원 구형 브라우저 폴백용 (nonce 지원 브라우저에서는 자동 무시됨)
        // 'strict-dynamic'은 신뢰된 스크립트가 동적으로 생성한 스크립트도 허용 (AdFit 등)
        [
            "script-src 'self'",
            `'nonce-${nonce}'`,
            "'strict-dynamic'",
            "'unsafe-inline'",
            ...(isDev ? ["'unsafe-eval'"] : []),
            "https://cdn.jsdelivr.net",
            "https://*.kakao.com",
            "https://accounts.kakao.com",
            "https://dapi.kakao.com",
            "https://display.ad.daum.net",
            "https://*.kakaocdn.net",
            "https://t1.daumcdn.net",
            "https://postfiles.pstatic.net",
            "https://aem-kakao-collector.onkakao.net",
            "https://www.gstatic.com",
            "https://www.gstatic.com/firebasejs/",
            "https://www.googletagmanager.com",
            "https://accounts.google.com",
            "http://clients2.google.com",
            "https://ssl.pstatic.net",
        ].join(' '),

        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net",

        "img-src 'self' data: https://*.kakaocdn.net https://postfiles.pstatic.net https://t1.daumcdn.net https://display.ad.daum.net https://kaat.daum.net https://serv.ds.kakao.com https://tr.ad.daum.net https://ssl.pstatic.net https://lh3.googleusercontent.com" +
            (isDev ? " http://*.kakaocdn.net" : ""),

        "font-src 'self' data: https://cdn.jsdelivr.net",

        "connect-src 'self' https://grow-farm.com ws://grow-farm.com" +
            (isDev ? " http://localhost:* ws://localhost:*" : "") +
            " https://cdn.jsdelivr.net https://*.kakao.com https://accounts.kakao.com https://dapi.kakao.com https://analytics.ad.daum.net https://display.ad.daum.net https://kaat.daum.net https://kuid-provider.ds.kakao.com https://t1.daumcdn.net https://aem-kakao-collector.onkakao.net https://www.google-analytics.com https://analytics.google.com https://accounts.google.com https://*.googleapis.com https://www.gstatic.com",

        "frame-src 'self' https://*.kakao.com https://accounts.kakao.com https://postfiles.pstatic.net https://t1.daumcdn.net https://analytics.ad.daum.net https://display.ad.daum.net about: chrome-extension:",

        "object-src 'none'",
        "base-uri 'self'",
        "form-action 'self' https://accounts.kakao.com https://sharer.kakao.com",
        "frame-ancestors 'self'",

        "media-src 'self' https://*.kakaocdn.net https://t1.daumcdn.net https://postfiles.pstatic.net",

        "child-src 'self' https://*.kakao.com https://accounts.kakao.com https://display.ad.daum.net about: chrome-extension:",
    ];

    const cspHeaderValue = cspDirectives.join('; ');

    const requestHeaders = new Headers(request.headers);
    requestHeaders.set('x-nonce', nonce);
    requestHeaders.set('Content-Security-Policy', cspHeaderValue);

    const response = NextResponse.next({
        request: {
            headers: requestHeaders,
        },
    });

    response.headers.set('Content-Security-Policy', cspHeaderValue);

    return response;
}

export const config = {
    matcher: [
        {
            source: '/((?!_next/static|_next/image|favicon\\.ico).*)',
            missing: [
                { type: 'header', key: 'next-router-prefetch' },
                { type: 'header', key: 'purpose', value: 'prefetch' },
            ],
        },
    ],
};
