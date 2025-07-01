import { Metadata } from "next";
import RollingPaperClient from "./rolling-paper-client";

export const metadata: Metadata = {
  title: "나의 롤링페이퍼",
  description:
    "친구들이 남긴 익명 메시지를 확인하고, 카카오톡으로 친구들에게 롤링페이퍼를 공유해보세요.",
  openGraph: {
    title: "나의 비밀로그 롤링페이퍼",
    description: "친구들이 남긴 익명 메시지를 확인해보세요!",
    url: "https://grow-farm.com/rolling-paper",
    siteName: "비밀로그",
    images: [
      {
        url: "/log.png",
        width: 800,
        height: 600,
        alt: "비밀로그 롤링페이퍼",
      },
    ],
    locale: "ko_KR",
    type: "website",
  },
};

export default function RollingPaperPage() {
  return <RollingPaperClient />;
}
