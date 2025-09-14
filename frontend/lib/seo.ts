// SEO 관련 유틸리티 함수들

export const generatePageTitle = (title: string) => {
  return title === "비밀로그" ? title : `${title} | 비밀로그`;
};

export const generateMetaDescription = (description: string) => {
  return description.length > 160 
    ? description.substring(0, 157) + "..."
    : description;
};

export const generateOpenGraphImage = (imagePath?: string) => {
  return imagePath 
    ? `https://grow-farm.com${imagePath}`
    : "https://grow-farm.com/log.png";
};

export const generateCanonicalUrl = (path: string) => {
  return `https://grow-farm.com${path}`;
};

export const generateStructuredData = {
  // 웹사이트 기본 정보
  website: {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: "비밀로그",
    url: "https://grow-farm.com",
    description: "친구들과 익명으로 소통하는 새로운 공간",
    publisher: {
      "@type": "Organization",
      name: "비밀로그",
      url: "https://grow-farm.com",
    },
    potentialAction: {
      "@type": "SearchAction",
      target: "https://grow-farm.com/board?q={search_term_string}",
      "query-input": "required name=search_term_string",
    },
  },

  // 게시글 구조화 데이터
  article: (title: string, content: string, author: string, publishedTime: string, url: string) => ({
    "@context": "https://schema.org",
    "@type": "Article",
    headline: title,
    description: content.replace(/<[^>]*>/g, '').substring(0, 160),
    author: {
      "@type": "Person",
      name: author,
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
    datePublished: publishedTime,
    dateModified: publishedTime,
    url: url,
    mainEntityOfPage: {
      "@type": "WebPage",
      "@id": url,
    },
    image: {
      "@type": "ImageObject",
      url: "https://grow-farm.com/log.png",
      width: 326,
      height: 105,
    },
  }),

  // 브레드크럼 구조화 데이터
  breadcrumb: (items: Array<{ title: string; href?: string }>) => ({
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.title,
      item: item.href ? `https://grow-farm.com${item.href}` : undefined,
    })),
  }),
};

// 기본 키워드 목록 (중복 제거 및 정리)
export const defaultKeywords = [
    // 핵심 브랜드 키워드
    "비밀로그",
    "익명 롤링페이퍼",
    "온라인 롤링페이퍼",

    // 주요 기능 키워드
    "생일축하메시지",
    "익명 메시지",
    "롤링페이퍼 사이트",
    "온라인 편지",
    "익명 커뮤니티",

    // 사용자 검색 키워드
    "감정쓰레기통",
    "커플테스트",
    "커플질문",
    "힘들때 위로 메시지",
    "나에 대한 질문",
    "여자친구 편지",
    "남자친구 편지",

    // 플랫폼 키워드
    "카카오톡 익명 메시지",
    "친구 익명 메시지",
    "익명으로 소통",
    "비밀로 메시지"
];

// 페이지별 키워드 생성
export const generateKeywords = (pageSpecificKeywords: string[] = []) => {
  return [...defaultKeywords, ...pageSpecificKeywords];
}; 