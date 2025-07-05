import { Metadata } from "next";

export const metadata: Metadata = {
  title: "마이페이지",
  description: "비밀로그에서 내 활동 내역과 설정을 관리하세요.",
  robots: "noindex, nofollow", // 개인 페이지는 검색 엔진에 색인되지 않도록 설정
};

export default function MyPageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
