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

// 기본 키워드 목록
export const defaultKeywords = [
    "생일축하메시지",
    "감정쓰레기통",
    "롤링페이퍼사이트",
    "생일축하메시지",
    "커플테스트",
    "커플질문",
    "힘들때위로",
    "나에대한질문",
    "여자친구편지",
    "남자친구편지",
    "롤링페이퍼사이트",
    "온라인편지",
    "익명속마음",
    "비밀로그",
    "익명",
    "롤링페이퍼",
    "커뮤니티",
    "게시판",
    "소통",
    "메시지",
    "카카오톡",
    "친구",
    "익명으로",
    "비밀로"
];

// 페이지별 키워드 생성
export const generateKeywords = (pageSpecificKeywords: string[] = []) => {
  return [...defaultKeywords, ...pageSpecificKeywords];
}; 