import { Metadata } from "next";
import { BoardClient } from "@/components/organisms/board";
import { generateStructuredData, generateKeywords, generateDynamicOgImage } from "@/lib/seo";

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

    return {
      title,
      description,
      keywords: generateKeywords([query, "검색", "검색 결과", "커뮤니티 검색"]),
      alternates: {
        canonical: `https://grow-farm.com/board?q=${encodeURIComponent(query)}`,
      },
      openGraph: {
        title: `${title} | 비밀로그`,
        description,
        url: `https://grow-farm.com/board?q=${encodeURIComponent(query)}`,
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

  // 페이지네이션이 있는 경우
  const currentPage = page ? parseInt(page) : 1;
  const baseTitle = currentPage > 1 ? `커뮤니티 게시판 (${currentPage}페이지)` : "커뮤니티 게시판";

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
      canonical: currentPage > 1
        ? `https://grow-farm.com/board?page=${currentPage}`
        : "https://grow-farm.com/board",
    },
    openGraph: {
      title: `${baseTitle} | 비밀로그`,
      description: "다른 사용자들과 소통하고 인기글을 확인해보세요.",
      url: currentPage > 1
        ? `https://grow-farm.com/board?page=${currentPage}`
        : "https://grow-farm.com/board",
      siteName: "비밀로그",
      images: [
        {
          url: "/bimillog_board_mobile.png",
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
      images: ["/bimillog_board_mobile.png"],
    },
  };
}

export default async function BoardPage({ searchParams }: Props) {
  const params = await searchParams;
  const query = params.q;

  // 검색 결과 페이지인 경우 구조화된 데이터 추가
  const searchJsonLd = query
    ? generateStructuredData.searchResultsPage(query, 0) // 실제 결과 수는 클라이언트에서 결정
    : null;

  // Breadcrumb 구조화 데이터
  const breadcrumbJsonLd = generateStructuredData.breadcrumb([
    { title: "홈", href: "/" },
    { title: query ? `"${query}" 검색 결과` : "커뮤니티", href: "/board" }
  ]);

  return (
    <>
      {searchJsonLd && (
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(searchJsonLd) }}
        />
      )}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbJsonLd) }}
      />
      <BoardClient />
    </>
  );
}
