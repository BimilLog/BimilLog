import type { NextConfig } from "next";
import withPWA from "next-pwa";
import path from "path";

const pwaConfig = {
    dest: "public",
    register: true,
    skipWaiting: true,
    disable: process.env.NODE_ENV === "development",
    // @ts-ignore
    importScripts: ["/firebase-messaging-sw.js"],
    buildExcludes: [/app-build-manifest\.json$/],
    runtimeCaching: [
        {
            urlPattern: /^https:\/\/grow-farm\.com\/api\/notification\/subscribe$/,
            handler: "NetworkOnly",
            options: {
                cacheName: "sse-bypass",
            },
        },
        {
            urlPattern: /^https:\/\/grow-farm\.com\/api\/(?!notification\/subscribe(?:\/|$))/,
            handler: "NetworkFirst",
            options: {
                cacheName: "api-cache",
                expiration: {
                    maxEntries: 32,
                    maxAgeSeconds: 24 * 60 * 60, // 24 hours
                },
            },
        },
        {
            urlPattern: /\.(?:png|jpg|jpeg|svg|gif|webp)$/,
            handler: "CacheFirst",
            options: {
                cacheName: "images",
                expiration: {
                    maxEntries: 64,
                    maxAgeSeconds: 7 * 24 * 60 * 60, // 7 days
                },
            },
        },
    ],
};

const nextConfig = withPWA(pwaConfig)({
    output: 'standalone',
    outputFileTracingRoot: path.join(__dirname, '../'),
    webpack: (config, { dev, isServer }) => {
        if (dev && !isServer) {
            // HMR 관련 파일 시스템 감시 설정 개선.
            config.watchOptions = {
                poll: 1000,
                aggregateTimeout: 300,
                ignored: /node_modules/,
            };

            // 파일 시스템 접근 오류 방지를 위한 설정
            config.optimization = {
                ...config.optimization,
                runtimeChunk: 'single',
            };
        }
        return config;
    },
    async redirects() {
        return [
            {
                source: '/admin/',
                destination: '/admin',
                permanent: true,
            },
        ];
    },
    headers: async () => {
        return [
            {
                source: '/.well-known/assetlinks.json',
                headers: [
                    {
                        key: 'Content-Type',
                        value: 'application/json',
                    },
                    {
                        key: 'Cache-Control',
                        value: 'public, max-age=3600',
                    },
                ],
            },
            {
                source: '/firebase-messaging-sw.js',
                headers: [
                    {
                        key: 'Service-Worker-Allowed',
                        value: '/',
                    },
                    {
                        key: 'Cache-Control',
                        value: 'public, max-age=0, must-revalidate',
                    },
                ],
            },
            {
                // admin 페이지에 Content-Type 헤더 명시적 설정
                source: '/admin/:path*',
                headers: [
                    {
                        key: 'Content-Type',
                        value: 'text/html; charset=utf-8',
                    },
                ],
            },
            {
                // 모든 페이지에 보안 헤더 적용
                source: '/:path*',
                headers: [
                    {
                        key: 'Content-Security-Policy',
                        value: [
                            "default-src 'self'",
                            // 스크립트 소스 허용 (개발환경에서만 unsafe-eval 허용)
                            "script-src 'self' 'unsafe-inline'" +
                            (process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : "") +
                            " https://cdn.jsdelivr.net https://*.kakao.com https://accounts.kakao.com https://dapi.kakao.com https://display.ad.daum.net https://*.kakaocdn.net https://t1.daumcdn.net https://postfiles.pstatic.net https://aem-kakao-collector.onkakao.net https://www.gstatic.com https://www.gstatic.com/firebasejs/ https://www.googletagmanager.com https://accounts.google.com http://clients2.google.com https://ssl.pstatic.net",
                            // 스타일시트 허용
                            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net",
                            // 이미지 소스 허용 (구체적 도메인만 명시)
                            "img-src 'self' data: https://*.kakaocdn.net https://postfiles.pstatic.net https://t1.daumcdn.net https://display.ad.daum.net https://kaat.daum.net https://serv.ds.kakao.com https://tr.ad.daum.net https://ssl.pstatic.net https://lh3.googleusercontent.com" +
                            (process.env.NODE_ENV === "development"
                                ? " http://*.kakaocdn.net"
                                : ""),
                            // 폰트 소스 허용
                            "font-src 'self' data: https://cdn.jsdelivr.net",
                            // API 연결 허용 (개발환경에서는 localhost 포함)
                            "connect-src 'self' https://grow-farm.com ws://grow-farm.com" +
                            (process.env.NODE_ENV === "development" ? " http://localhost:* ws://localhost:*" : "") +
                            " https://cdn.jsdelivr.net https://*.kakao.com https://accounts.kakao.com https://dapi.kakao.com https://analytics.ad.daum.net https://display.ad.daum.net https://kaat.daum.net https://kuid-provider.ds.kakao.com https://t1.daumcdn.net https://aem-kakao-collector.onkakao.net https://www.google-analytics.com https://analytics.google.com https://accounts.google.com https://*.googleapis.com https://www.gstatic.com",
                            // 프레임 허용 (구체적 도메인만 명시)
                            "frame-src 'self' https://*.kakao.com https://accounts.kakao.com https://postfiles.pstatic.net https://t1.daumcdn.net https://analytics.ad.daum.net https://display.ad.daum.net about: chrome-extension:",
                            "object-src 'none'",
                            "base-uri 'self'",
                            "form-action 'self' https://accounts.kakao.com https://sharer.kakao.com",
                            // 클릭재킹 방지 (X-Frame-Options와 중복이지만 더 정확한 제어)
                            "frame-ancestors 'self'",
                            // 미디어 소스 허용
                            "media-src 'self' https://*.kakaocdn.net https://t1.daumcdn.net https://postfiles.pstatic.net",
                            // 카카오 공유 팝업 허용
                            "child-src 'self' https://*.kakao.com https://accounts.kakao.com https://display.ad.daum.net about: chrome-extension:",
                        ].join('; '),
                    },
                    {
                        key: 'X-XSS-Protection',
                        value: '1; mode=block',
                    },
                    {
                        key: 'X-Content-Type-Options',
                        value: 'nosniff',
                    },
                    {
                        key: 'Referrer-Policy',
                        value: 'strict-origin-when-cross-origin',
                    },
                    {
                        // 클릭재킹 방지
                        key: 'X-Frame-Options',
                        value: 'SAMEORIGIN',
                    },
                    // HTTPS 강제 (HSTS) - production에서만 적용
                    ...(process.env.NODE_ENV === "production" ? [{
                        key: 'Strict-Transport-Security',
                        value: 'max-age=31536000; includeSubDomains',
                    }] : []),
                ],
            },
        ];
    },
    images: {
        remotePatterns: [
            {
                protocol: 'https',
                hostname: '*.kakaocdn.net',
            },
            {
                protocol: 'https',
                hostname: '*.kakao.com',
            },
            {
                protocol: 'https',
                hostname: 'lh3.googleusercontent.com',
            },
        ],
        formats: ['image/avif', 'image/webp'],
        deviceSizes: [640, 768, 1024, 1280, 1536],
        imageSizes: [16, 32, 48, 64, 96, 128, 256],
        minimumCacheTTL: 60 * 60 * 24 * 365, // 1년 캐싱
        dangerouslyAllowSVG: false,
        contentDispositionType: 'attachment',
        contentSecurityPolicy: "default-src 'self'; script-src 'none'; sandbox;",
    },
    eslint: {
        ignoreDuringBuilds: true,
    },
}) as NextConfig;

export default nextConfig;
