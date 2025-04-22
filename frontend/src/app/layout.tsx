import type { Metadata } from "next";
import "./globals.css";
import Link from "next/link";
import AuthCheck from "@/components/AuthCheck";
import Navigation from "@/components/Navigation";
import Script from "next/script";

export const metadata: Metadata = {
  title: "농장 키우기",
  description: "농장 키우기 웹 애플리케이션",
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
          href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css"
          rel="stylesheet"
        />
      </head>
      <body className="d-flex flex-column h-100">
        <AuthCheck>
          <Navigation />
          <div className="flex-grow-1 d-flex flex-column">{children}</div>
          <Footer />
        </AuthCheck>
        <Script
          src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
          strategy="afterInteractive"
        />
      </body>
    </html>
  );
}
