import { Metadata } from "next";
import { generateKeywords } from "@/lib/seo";
import RollingPaperClient from "./rolling-paper-client";

export const metadata: Metadata = {
  title: "나의 롤링페이퍼",
  description:
    "친구들이 남긴 익명 메시지를 확인하고, 카카오톡으로 친구들에게 롤링페이퍼를 공유해보세요. 다양한 디자인의 메시지들을 모아서 따뜻한 추억을 만들어보세요.",
  keywords: generateKeywords([
      "롤링페이퍼",
"롤링페이퍼사이트",
"생일축하메시지",
"커플테스트",

    "나의 롤링페이퍼",
    "내 롤링페이퍼",
    "익명 메시지",
    "친구 메시지",
    "카카오톡 공유",
    "메시지 모음",
    "롤링페이퍼 관리",
    "카카오톡",
    "카톡",
    "카톡 친구 익명",
    "카톡 익명",
  ]),
  openGraph: {
    title: "나의 비밀로그 롤링페이퍼",
    description: "친구들이 남긴 익명 메시지를 확인해보세요!",
    url: "https://grow-farm.com/rolling-paper",
    siteName: "비밀로그",
    images: [
      {
        url: "/bimillog_mypaper_mobile.png",
        alt: "비밀로그 롤링페이퍼",
      },
    ],
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "나의 비밀로그 롤링페이퍼",
    description: "친구들이 남긴 익명 메시지를 확인해보세요!",
    images: ["/bimillog_mypaper_mobile.png"],
  },
};

// 구조화된 데이터
const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebPage",
  name: "나의 롤링페이퍼",
  description:
    "친구들이 남긴 익명 메시지를 확인하고 관리할 수 있는 개인 롤링페이퍼 공간",
  url: "https://grow-farm.com/rolling-paper",
  mainEntity: {
    "@type": "CollectionPage",
    name: "롤링페이퍼 메시지 모음",
    description: "친구들이 남긴 다양한 디자인의 익명 메시지들",
  },
  breadcrumb: {
    "@type": "BreadcrumbList",
    itemListElement: [
      {
        "@type": "ListItem",
        position: 1,
        name: "홈",
        item: "https://grow-farm.com",
      },
      {
        "@type": "ListItem",
        position: 2,
        name: "나의 롤링페이퍼",
        item: "https://grow-farm.com/rolling-paper",
      },
    ],
  },
};

export default function RollingPaperPage() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(jsonLd),
        }}
      />
      <RollingPaperClient />
    </>
  );
}
