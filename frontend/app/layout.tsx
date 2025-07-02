import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Script from "next/script";
import "./globals.css";
import { AuthProvider } from "@/hooks/useAuth";
import { BrowserGuideWrapper } from "@/components/organisms/browser-guide-wrapper";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#000000",
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
  ],
  authors: [{ name: "비밀로그 Team", url: "https://grow-farm.com" }],
  robots: "index, follow",
  openGraph: {
    title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
    description:
      "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요.",
    url: "https://grow-farm.com",
    siteName: "비밀로그",
    images: [
      {
        url: "/log.png",
        width: 800,
        height: 600,
        alt: "비밀로그 로고",
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
    images: ["/log.png"],
  },
  icons: {
    icon: "/favicon.png",
    shortcut: "/favicon.png",
    apple: "/favicon.png",
  },
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "비밀로그",
  },
  applicationName: "비밀로그",
  verification: {
    google: "h46_QB3B0te_apY6uiYRUUOuSEt-S8_nQgHo5Iwcv0E",
    other: {
      "naver-site-verification": "c97ee92e3cbcbdae8b09fa0a849758b0dd759675",
    },
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {/* Google Analytics */}
        <Script
          src="https://www.googletagmanager.com/gtag/js?id=G-G9C4KYCEEJ"
          strategy="afterInteractive"
        />
        <Script id="google-analytics" strategy="afterInteractive">
          {`
            window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', 'G-G9C4KYCEEJ');
          `}
        </Script>

        <AuthProvider>
          <BrowserGuideWrapper>{children}</BrowserGuideWrapper>
        </AuthProvider>
      </body>
    </html>
  );
}
