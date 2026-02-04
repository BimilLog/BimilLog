import { Metadata } from "next";
import { headers } from "next/headers";
import { BoardClient } from "@/components/organisms/board";
import { generateStructuredData, generateKeywords, generateDynamicOgImage } from "@/lib/seo";
import { getBoardInitialData, getSearchInitialData } from "@/lib/api/server";

type Props = {
  searchParams: Promise<{
    q?: string;
    type?: string;
    page?: string;
  }>;
};

// 동적 메타데이터 생성 (검색 쿼리 포함)
export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const params = await searchParams;
  const query = params.q;
  const page = params.page;

  // 검색 결과 페이지인 경우
  if (query) {
    const title = `"${query}" 검색 결과`;
    const description = `비밀로그 커뮤니티에서 "${query}"에 대한 검색 결과를 확인하세요.`;
    const searchPage = page ? parseInt(page) : 1;

    return {
      title,
      description,
      keywords: generateKeywords([query, "검색", "검색 결과", "커뮤니티 검색"]),
      alternates: {
        canonical: searchPage > 1
          ? `https://grow-farm.com/board?q=${encodeURIComponent(query)}&page=${searchPage}`
          : `https://grow-farm.com/board?q=${encodeURIComponent(query)}`,
        ...(searchPage > 1 && {
          prev: searchPage === 2
            ? `https://grow-farm.com/board?q=${encodeURIComponent(query)}`
            : `https://grow-farm.com/board?q=${encodeURIComponent(query)}&page=${searchPage - 1}`
        }),
      },
      openGraph: {
        title: `${title} | 비밀로그`,
        description,
        url: searchPage > 1
          ? `https://grow-farm.com/board?q=${encodeURIComponent(query)}&page=${searchPage}`
          : `https://grow-farm.com/board?q=${encodeURIComponent(query)}`,
        siteName: "비밀로그",
        images: [
          {
            url: generateDynamicOgImage({
              title: `"${query}" 검색 결과`,
              type: 'default'
            }),
            width: 1200,
            height: 630,
            alt: title,
          },
        ],
        locale: "ko_KR",
        type: "website",
      },
      twitter: {
        card: "summary_large_image",
        title: `${title} | 비밀로그`,
        description,
        images: [generateDynamicOgImage({
          title: `"${query}" 검색 결과`,
          type: 'default'
        })],
      },
      robots: {
        index: true,
        follow: true,
      },
    };
  }

  // 일반 게시판 페이지 (커서 기반이므로 page 파라미터 없음)
  const baseTitle = "커뮤니티 게시판";

  // 일반 게시판 페이지용 동적 OG 이미지
  const boardOgImage = generateDynamicOgImage({
    title: baseTitle,
    type: 'default'
  });

  return {
    title: baseTitle,
    description:
      "비밀로그 커뮤니티에서 다른 사용자들과 자유롭게 소통하고 생각을 나눠보세요. 실시간, 주간, 레전드 인기글도 확인할 수 있습니다.",
    keywords: [
      "비밀로그 커뮤니티",
      "익명 게시판",
      "실시간 인기글",
      "주간 인기글",
      "레전드 게시글",
      "익명 소통",
      "비밀로그 게시판",
      "온라인 커뮤니티",
      "익명 커뮤니티 게시판"
    ],
    alternates: {
      canonical: "https://grow-farm.com/board",
    },
    openGraph: {
      title: `${baseTitle} | 비밀로그`,
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요.",
      url: "https://grow-farm.com/board",
      siteName: "비밀로그",
      images: [
        {
          url: boardOgImage,
          width: 1200,
          height: 630,
          alt: "비밀로그 게시판",
        },
      ],
      locale: "ko_KR",
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: `${baseTitle} | 비밀로그`,
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요.",
      images: [boardOgImage],
    },
  };
}

export default async function BoardPage({ searchParams }: Props) {
  const params = await searchParams;
  const query = params.q;
  const searchType = (params.type as 'TITLE' | 'TITLE_CONTENT' | 'WRITER') || 'TITLE';
  const page = params.page ? parseInt(params.page) - 1 : 0; // URL은 1-based, API는 0-based (검색용)

  // SSR: 서버에서 초기 데이터 fetch (내부 통신)
  // 검색 쿼리가 있으면 검색 결과 (offset 기반), 없으면 일반 목록 (cursor 기반)
  const initialData = query
    ? await getSearchInitialData(searchType, query, page, 20)
    : await getBoardInitialData(20);  // cursor 기반은 page 파라미터 불필요

  // 검색 결과 페이지인 경우 구조화된 데이터 추가
  const searchJsonLd = query
    ? generateStructuredData.searchResultsPage(query, 0) // 실제 결과 수는 클라이언트에서 결정
    : null;

  // Breadcrumb 구조화 데이터
  const breadcrumbJsonLd = generateStructuredData.breadcrumb([
    { title: "홈", href: "/" },
    { title: query ? `"${query}" 검색 결과` : "커뮤니티", href: "/board" }
  ]);

  const nonce = (await headers()).get('x-nonce') ?? '';

  return (
    <>
      {searchJsonLd && (
        <script
          type="application/ld+json"
          nonce={nonce}
          dangerouslySetInnerHTML={{ __html: JSON.stringify(searchJsonLd) }}
        />
      )}
      <script
        type="application/ld+json"
        nonce={nonce}
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbJsonLd) }}
      />
      <BoardClient initialData={initialData} />
    </>
  );
}
