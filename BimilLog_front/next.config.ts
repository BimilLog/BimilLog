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
            // SSE 알림은 브라우저에서 직접 백엔드로 연결 (Server Action 불가)
            urlPattern: /^https:\/\/grow-farm\.com\/api\/notification\/subscribe$/,
            handler: "NetworkOnly",
            options: {
                cacheName: "sse-bypass",
            },
        },
        {
            // API 호출은 Route Handler로 프록시되므로 NetworkFirst 전략 사용
            urlPattern: /\/api\//,
            handler: "NetworkFirst",
            options: {
                cacheName: "api-cache",
                expiration: {
                    maxEntries: 50,
                    maxAgeSeconds: 60, // 1분 캐시
                },
                networkTimeoutSeconds: 10,
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
    // API 프록시: app/api/[...path]/route.ts Route Handler가 처리
    // 브라우저 → Next.js Route Handler → 백엔드 (내부 통신)
    // SSE 알림만 브라우저에서 직접 백엔드로 연결 (lib/api/sse.ts)
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
                // CSP는 middleware.ts에서 nonce 기반으로 동적 설정
                source: '/:path*',
                headers: [
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
