import type { Metadata, Viewport } from "next";
import { headers } from "next/headers";
import { Geist, Geist_Mono } from "next/font/google";
import Script from "next/script";
import "./globals.css";
import { ClientProviders } from "@/providers/client-providers";
import { QueryProvider } from "@/providers/query-provider";
import { ThemeProvider } from "@/providers/theme-provider";
import { WebVitalsReporter } from "@/components/analytics/web-vitals";
import { ErrorInitializer } from "@/components/error-initializer";
import { TestCrashButton } from "@/components/test-crash-button";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: 'swap',
  preload: true,
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: 'swap',
  preload: true,
});

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#000000",
  viewportFit: "cover", // Android 15 edge-to-edge 지원
};

export const metadata: Metadata = {
  metadataBase: new URL("https://grow-farm.com"),
  title: {
    default: "비밀로그",
    template: "%s | 비밀로그",
  },
  description:
    "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요. 익명 롤링페이퍼와 커뮤니티 기능을 통해 일상의 재미를 더할 수 있습니다.",
  keywords: [
    "롤링페이퍼",
    "비밀로그",
    "커뮤니티",
    "게시판",
    "메시지",
    "익명",
    "카카오톡",
    "친구",
    "소통",
    "익명 SNS"
  ],
  authors: [{ name: "비밀로그 Team", url: "https://grow-farm.com" }],
  creator: "비밀로그 Team",
  publisher: "비밀로그",
  robots: "index, follow",
  openGraph: {
    title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
    description:
      "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요.",
    url: "https://grow-farm.com",
    siteName: "비밀로그",
    images: [
      {
        url: "/bimillog_mainpage_pc.png",
        alt: "비밀로그 PC 메인 페이지",
      },
      {
        url: "/bimillog_mainpage_mobile.png",
        alt: "비밀로그 모바일 메인 페이지",
      },
    ],
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
    description:
      "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요.",
    images: ["/bimillog_mainpage_pc.png"],
  },
  icons: {
    icon: [
      { url: "/favicon48.png", sizes: "48x48", type: "image/png" },
      { url: "/favicon96.png", sizes: "96x96", type: "image/png" },
      { url: "/favicon192.png", sizes: "192x192", type: "image/png" },
      { url: "/favicon512.png", sizes: "512x512", type: "image/png" },
    ],
    apple: [
      { url: "/favicon192.png", sizes: "192x192", type: "image/png" },
      { url: "/favicon512.png", sizes: "512x512", type: "image/png" },
    ],
  },
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "비밀로그",
  },
  applicationName: "비밀로그",
  category: "social",
  verification: {
    google: "h46_QB3B0te_apY6uiYRUUOuSEt-S8_nQgHo5Iwcv0E",
    other: {
      "naver-site-verification": "c97ee92e3cbcbdae8b09fa0a849758b0dd759675",
    },
  },
  other: {
    "mobile-web-app-capable": "yes",
    "apple-mobile-web-app-capable": "yes",
    "apple-mobile-web-app-status-bar-style": "default",
    "apple-mobile-web-app-title": "비밀로그",
  },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const nonce = (await headers()).get('x-nonce') ?? '';

  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        {/* Preconnect & DNS Prefetch */}
        <link rel="preconnect" href="https://grow-farm.com" />
        <link rel="dns-prefetch" href="https://grow-farm.com" />
        <link rel="preconnect" href="https://accounts.kakao.com" />
        <link rel="dns-prefetch" href="https://accounts.kakao.com" />
        <link rel="preconnect" href="https://www.googletagmanager.com" />
        <link rel="dns-prefetch" href="https://www.googletagmanager.com" />

        {/* Favicons */}
        <link rel="icon" href="/favicon48.png" sizes="48x48" type="image/png" />
        <link rel="icon" href="/favicon96.png" sizes="96x96" type="image/png" />
        <link
          rel="icon"
          href="/favicon192.png"
          sizes="192x192"
          type="image/png"
        />
        <link
          rel="icon"
          href="/favicon512.png"
          sizes="512x512"
          type="image/png"
        />
        <link rel="apple-touch-icon" href="/favicon192.png" />
        <link rel="apple-touch-icon" sizes="192x192" href="/favicon192.png" />
        <link rel="apple-touch-icon" sizes="512x512" href="/favicon512.png" />
        <meta name="theme-color" content="#000000" />
        <meta name="application-name" content="비밀로그" />
        <meta name="apple-mobile-web-app-title" content="비밀로그" />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {/* Google Analytics */}
        <Script
          src="https://www.googletagmanager.com/gtag/js?id=G-G9C4KYCEEJ"
          strategy="afterInteractive"
          nonce={nonce}
        />
        <Script id="google-analytics" strategy="afterInteractive" nonce={nonce}>
          {`
            window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', 'G-G9C4KYCEEJ');
          `}
        </Script>

        {/* Web Vitals 모니터링 */}
        <WebVitalsReporter />

        {/* 전역 에러 핸들러 초기화 */}
        <ErrorInitializer />

        <ThemeProvider>
          <QueryProvider>
            <ClientProviders>{children}</ClientProviders>
          </QueryProvider>
        </ThemeProvider>

        {/* 개발 모드 전용 테스트 크래시 버튼 */}
        <TestCrashButton />
      </body>
    </html>
  );
}
