import { Metadata } from "next";

export const metadata: Metadata = {
  title: "설정 | 비밀로그",
  description: "비밀로그 알림 설정과 계정을 관리하세요.",
  robots: "noindex, nofollow",
  openGraph: {
    title: "설정",
    description: "비밀로그 알림 설정과 계정을 관리하세요.",
    type: "website",
  },
};

export default function SettingsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}