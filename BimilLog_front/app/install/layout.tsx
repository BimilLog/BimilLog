import { Metadata } from "next";

export const metadata: Metadata = {
  title: "앱 설치 - 비밀로그",
  description: "비밀로그를 앱으로 설치하여 더 빠르고 편리하게 이용하세요. PWA 설치 가이드를 제공합니다.",
};

export default function InstallLayout({ children }: { children: React.ReactNode }) {
  return children;
}
