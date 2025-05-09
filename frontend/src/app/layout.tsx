import type { Metadata, Viewport } from "next";
import "./globals.css";
import Link from "next/link";
import AuthCheck from "@/components/AuthCheck";
import Navigation from "@/components/Navigation";
import Script from "next/script";

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  title: "농장 키우기 - 카카오 친구들과 함께하는 가상 농장",
  description:
    "카카오 친구들과 함께 익명 메시지로 마음을 담아 농장을 꾸며보세요. 친구의 농장을 방문하고 소통할 수 있는 농장 키우기 서비스입니다.",
  keywords: [
    "농장",
    "카카오",
    "메시지",
    "익명",
    "소통",
    "친구",
    "가상 농장",
    "농장 키우기",
  ],
  authors: [{ name: "농장 키우기 팀" }],
  robots: "index, follow",
  metadataBase: new URL("https://grow-farm.com"), // 실제 도메인으로 수정하세요
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "ko_KR",
    url: "https://grow-farm.com", // 실제 도메인으로 수정하세요
    title: "농장 키우기 - 카카오 친구들과 함께하는 가상 농장",
    description:
      "카카오 친구들과 함께 익명 메시지로 마음을 담아 농장을 꾸며보세요. 친구의 농장을 방문하고 소통할 수 있는 농장 키우기 서비스입니다.",
    siteName: "농장 키우기",
    images: [
      {
        url: "/farmImage.jpeg", // 대표 이미지 경로
        width: 1200,
        height: 630,
        alt: "농장 키우기 대표 이미지",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "농장 키우기 - 카카오 친구들과 함께하는 가상 농장",
    description:
      "카카오 친구들과 함께 익명 메시지로 마음을 담아 농장을 꾸며보세요. 친구의 농장을 방문하고 소통할 수 있는 농장 키우기 서비스입니다.",
    images: ["/farmImage.jpeg"],
    creator: "",
  },
  icons: {
    icon: "/app_icon.jpg",
    apple: "/app_icon.jpg",
  },
  manifest: "/manifest.json",
  category: "social",
};

// 푸터 컴포넌트
const Footer = () => {
  return (
    <footer className="bg-dark py-4 mt-auto">
      <div className="container px-5">
        <div className="row align-items-center justify-content-between flex-column flex-sm-row">
          <div className="col-auto">
            <div className="small m-0 text-white">
              Copyright &copy; 농장 키우기 {new Date().getFullYear()}
            </div>
          </div>
          <div className="col-auto">
            <Link className="link-light small" href="/privacy">
              개인정보처리방침
            </Link>
            <span className="text-white mx-1">&middot;</span>
            <Link className="link-light small" href="/terms">
              이용약관
            </Link>
            <span className="text-white mx-1">&middot;</span>
            <Link
              className="link-light small"
              href="https://www.notion.so/1d4a9f47800c80a1b12fc2aae7befd0e?pvs=4"
            >
              개발자
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-100">
      <head>
        <link
          rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
          crossOrigin="anonymous"
        />
        <link
          rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css"
          crossOrigin="anonymous"
        />
      </head>
      <body className="d-flex flex-column h-100">
        <AuthCheck>
          <Navigation />
          <div className="flex-grow-1 d-flex flex-column">{children}</div>
          <Footer />
        </AuthCheck>
        <Script
          src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"
          strategy="afterInteractive"
          crossOrigin="anonymous"
        />
      </body>
    </html>
  );
}
