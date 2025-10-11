import { Metadata } from "next";
import { HomeClient } from "@/components/organisms/home";

export const metadata: Metadata = {
  title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
  description:
    "비밀로그에서 익명으로 롤링페이퍼를 만들고 친구들과 솔직한 메시지를 나눠보세요. 카카오톡으로 간편하게 시작할 수 있습니다.",
  keywords: [
    "비밀로그",
    "익명 롤링페이퍼",
    "온라인 롤링페이퍼",
    "생일축하메시지",
    "익명 커뮤니티",
    "카카오톡 익명 메시지",
    "친구 익명 메시지",
    "감정쓰레기통",
    "온라인 편지",
    "비밀로 메시지"
  ],
  alternates: {
    canonical: "https://grow-farm.com",
  },
  openGraph: {
    title: "비밀로그 - 익명 롤링페이퍼 & 커뮤니티",
    description:
      "친구들과 익명으로 소통하는 새로운 공간, 비밀로그에서 솔직한 마음을 나눠보세요.",
    url: "https://grow-farm.com",
    siteName: "비밀로그",
    images: [
      {
        url: "/bimillog_mainpage_pc.png",
        alt: "비밀로그 메인 페이지",
      },
      {
        url: "/bimillog_mainpage_mobile.png",
        alt: "비밀로그 모바일 메인 페이지",
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
    images: ["/bimillog_mainpage_pc.png"],
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
      url: "/bimillog_mainpage_pc.png",
      width: 1200,
      height: 630,
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
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(jsonLd),
        }}
      />
      <HomeClient />
    </>
  );
}
