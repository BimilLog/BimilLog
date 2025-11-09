import { Metadata } from "next";
import { generateKeywords } from "@/lib/seo";
import { VisitClient } from "@/components/organisms/rolling-paper/visit";

export const metadata: Metadata = {
  title: "롤링페이퍼 방문",
  description:
    "친구의 롤링페이퍼를 방문하여 따뜻한 익명 메시지를 남겨보세요. 닉네임을 검색하여 롤링페이퍼를 찾고 다양한 디자인으로 메시지를 꾸미세요.",
  keywords: generateKeywords([
      "롤링페이퍼",
"생일축하메시지",

    "롤링페이퍼 방문",
    "익명 메시지",
    "친구 롤링페이퍼",
    "메시지 남기기",
    "롤링페이퍼 검색",
    "닉네임 검색",
    "친구 롤링페이퍼",
    "익명 메시지",
    "메시지 남기기",
    "롤링페이퍼 메시지",
    "카톡",
    "카톡 친구",
    "카톡 친구 익명",
    "친구에게 익명 메시지",
    "익명 롤링페이퍼",
    "카톡 친구 익명 메시지",
  ]),
  robots: "index, follow",
  alternates: {
    canonical: "https://grow-farm.com/visit",
  },
  authors: [{ name: "비밀로그 Team", url: "https://grow-farm.com" }],
  creator: "비밀로그",
  publisher: "비밀로그",
  openGraph: {
    title: "롤링페이퍼 방문 | 비밀로그",
    description:
      "친구의 롤링페이퍼를 방문하여 따뜻한 익명 메시지를 남겨보세요.",
    url: "https://grow-farm.com/visit",
    siteName: "비밀로그",
    images: [
      {
        url: "/bimillog_visit_mobile.png",
        alt: "비밀로그 롤링페이퍼 방문",
      },
    ],
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "롤링페이퍼 방문 | 비밀로그",
    description:
      "친구의 롤링페이퍼를 방문하여 따뜻한 익명 메시지를 남겨보세요.",
    images: ["/bimillog_visit_mobile.png"],
  },
};

// 구조화된 데이터
const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebPage",
  name: "롤링페이퍼 방문",
  description: "친구의 롤링페이퍼를 방문하여 따뜻한 익명 메시지를 남겨보세요",
  url: "https://grow-farm.com/visit",
  inLanguage: "ko",
  isPartOf: {
    "@type": "WebSite",
    name: "비밀로그",
    url: "https://grow-farm.com",
  },
  mainEntity: {
    "@type": "SearchAction",
    target: "https://grow-farm.com/rolling-paper/{nickname}",
    "query-input": "required name=nickname",
    description: "닉네임으로 롤링페이퍼 검색",
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
        name: "롤링페이퍼 방문",
        item: "https://grow-farm.com/visit",
      },
    ],
  },
  potentialAction: {
    "@type": "SearchAction",
    target: "https://grow-farm.com/rolling-paper/{search_term_string}",
    "query-input": "required name=search_term_string",
  },
};

export default function VisitPage() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(jsonLd),
        }}
      />
      <VisitClient />
    </>
  );
}
