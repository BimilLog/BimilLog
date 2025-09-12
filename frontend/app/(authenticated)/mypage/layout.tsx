import { Metadata } from "next";

export const metadata: Metadata = {
  title: "마이페이지 | 비밀로그",
  description: "비밀로그에서 내 활동 내역과 설정을 관리하세요.",
  robots: "noindex, nofollow",
  openGraph: {
    title: "마이페이지",
    description: "비밀로그에서 내 활동 내역과 설정을 관리하세요.",
    type: "website",
  },
};

export default function MyPageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}