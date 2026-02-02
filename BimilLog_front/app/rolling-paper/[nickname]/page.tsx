import { Metadata, ResolvingMetadata } from "next";
import { headers } from "next/headers";
import { generateKeywords, generateDynamicOgImage } from "@/lib/seo";
import { RollingPaperClient } from "@/components/organisms/rolling-paper";
import { getRollingPaperServer } from "@/lib/api/server";

type Props = {
  params: Promise<{ nickname: string }>;
};

export async function generateMetadata(
  { params }: Props,
  parent: ResolvingMetadata
): Promise<Metadata> {
  const { nickname } = await params;
  const decodedNickname = decodeURIComponent(nickname);

  const previousImages = (await parent).openGraph?.images || [];

  // 동적 OG 이미지 생성
  const ogImageUrl = generateDynamicOgImage({
    title: `${decodedNickname}님의 롤링페이퍼`,
    author: decodedNickname,
    type: 'paper'
  });

  return {
    title: `${decodedNickname}님의 롤링페이퍼`,
    description: `${decodedNickname}님에게 익명으로 따뜻한 메시지를 남겨보세요. 로그인 없이도 다양한 디자인으로 메시지를 꾸며 보낼 수 있습니다.`,
    keywords: generateKeywords([
      `${decodedNickname}님의 롤링페이퍼`,
      `${decodedNickname} 롤링페이퍼`,
      "생일축하메시지",

      "친구 롤링페이퍼",
      "익명 메시지",
      "메시지 남기기",
      "롤링페이퍼 메시지",
      "카톡",
      "카톡 친구",
      "카톡 친구 익명",
      "친구에게 익명 메시지",
      "익명 롤링페이퍼",
      "카톡 친구 익명메시지",
    ]),
    alternates: {
      canonical: `https://grow-farm.com/rolling-paper/${nickname}`,
    },
    openGraph: {
      title: `${decodedNickname}님의 비밀로그 롤링페이퍼`,
      description: `친구 ${decodedNickname}님에게 익명으로 메시지를 남겨보세요!`,
      url: `https://grow-farm.com/rolling-paper/${nickname}`,
      siteName: "비밀로그",
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: `${decodedNickname}님의 롤링페이퍼`,
        },
        ...previousImages,
      ],
      locale: "ko_KR",
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: `${decodedNickname}님의 비밀로그 롤링페이퍼`,
      description: `친구 ${decodedNickname}님에게 익명으로 메시지를 남겨보세요!`,
      images: [ogImageUrl],
    },
  };
}

export default async function PublicRollingPaperPage({
  params,
}: {
  params: Promise<{ nickname: string }>;
}) {
  const { nickname } = await params;
  const decodedNickname = decodeURIComponent(nickname);

  // SSR: 내부 IP로 롤링페이퍼 데이터 조회
  const initialPaperData = await getRollingPaperServer(decodedNickname);

  // 구조화된 데이터
  const pageJsonLd = {
    "@context": "https://schema.org",
    "@type": "WebPage",
    name: `${decodedNickname}님의 롤링페이퍼`,
    description: `${decodedNickname}님에게 익명으로 따뜻한 메시지를 남기는 롤링페이퍼`,
    url: `https://grow-farm.com/rolling-paper/${nickname}`,
    mainEntity: {
      "@type": "CreativeWork",
      name: `${decodedNickname}님의 롤링페이퍼`,
      description: "친구들이 남길 수 있는 익명 메시지 모음",
      author: {
        "@type": "Person",
        name: decodedNickname,
      },
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
        {
          "@type": "ListItem",
          position: 3,
          name: `${decodedNickname}님의 롤링페이퍼`,
          item: `https://grow-farm.com/rolling-paper/${nickname}`,
        },
      ],
    },
  };

  const nonce = (await headers()).get('x-nonce') ?? '';

  return (
    <>
      <script
        type="application/ld+json"
        nonce={nonce}
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(pageJsonLd),
        }}
      />
      <RollingPaperClient nickname={nickname} initialPaperData={initialPaperData?.data ?? undefined} />
    </>
  );
}
