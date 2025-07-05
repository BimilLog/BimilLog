import { Metadata } from "next";

export const metadata: Metadata = {
  title: "로그인",
  description: "비밀로그에 로그인하여 나만의 롤링페이퍼를 만들어보세요.",
  robots: "noindex, nofollow", // 검색 엔진에 색인되지 않도록 설정
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
