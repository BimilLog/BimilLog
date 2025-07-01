import { Metadata } from "next";
import HomeClient from "./home-client";

export const metadata: Metadata = {
  title: "비밀로그 | 익명 롤링페이퍼와 커뮤니티",
  description:
    "비밀로그에서 익명으로 롤링페이퍼를 만들고 친구들과 솔직한 메시지를 나눠보세요. 카카오톡으로 간편하게 시작할 수 있습니다.",
  openGraph: {
    title: "비밀로그 | 익명 롤링페이퍼와 커뮤니티",
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
};

export default function HomePage() {
  return <HomeClient />;
}
