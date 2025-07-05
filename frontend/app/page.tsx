import { Metadata } from "next";
import Script from "next/script";
import HomeClient from "./home-client";

export const metadata: Metadata = {
  title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
  description:
    "비밀로그에서 익명으로 롤링페이퍼를 만들고 친구들과 솔직한 메시지를 나눠보세요. 카카오톡으로 간편하게 시작할 수 있습니다.",
  keywords: [
    "롤링페이퍼",
    "비밀로그",
    "커뮤니티",
    "게시판",
    "메시지",
    "익명",
    "카카오톡",
    "친구",
    "소통",
  ],
  openGraph: {
    title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
    description:
      "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요.",
    url: "https://grow-farm.com",
    siteName: "비밀로그",
    images: [
      {
        url: "https://grow-farm.com/log.png",
        width: 326,
        height: 105,
        alt: "비밀로그 로고",
      },
      {
        url: "https://grow-farm.com/favicon512.png",
        width: 512,
        height: 512,
        alt: "비밀로그 아이콘",
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
    images: ["https://grow-farm.com/log.png"],
  },
};

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: "비밀로그",
  alternateName: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
  url: "https://grow-farm.com",
  description:
    "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요. 익명 롤링페이퍼와 커뮤니티 기능을 통해 일상의 재미를 더할 수 있습니다.",
  sameAs: ["https://grow-farm.com"],
  potentialAction: {
    "@type": "SearchAction",
    target: "https://grow-farm.com/board?q={search_term_string}",
    "query-input": "required name=search_term_string",
  },
  publisher: {
    "@type": "Organization",
    name: "비밀로그",
    url: "https://grow-farm.com",
    logo: {
      "@type": "ImageObject",
      url: "https://grow-farm.com/log.png",
      width: 326,
      height: 105,
    },
  },
  mainEntity: {
    "@type": "WebApplication",
    name: "비밀로그",
    url: "https://grow-farm.com",
    applicationCategory: "SocialNetworkingApplication",
    operatingSystem: "Web",
    description:
      "익명 롤링페이퍼와 커뮤니티 기능을 제공하는 소셜 네트워킹 웹 애플리케이션",
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "KRW",
    },
  },
};

export default function HomePage() {
  return (
    <>
      <Script
        id="json-ld"
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(jsonLd),
        }}
      />
      <HomeClient />
    </>
  );
}
