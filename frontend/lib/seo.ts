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
      target: {
        "@type": "EntryPoint",
        urlTemplate: "https://grow-farm.com/board?q={search_term_string}"
      },
      "query-input": "required name=search_term_string",
    },
  },

  // 향상된 조직 정보
  organization: {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: "비밀로그",
    url: "https://grow-farm.com",
    logo: {
      "@type": "ImageObject",
      url: "https://grow-farm.com/log.png",
      width: 326,
      height: 105,
    },
    sameAs: [
      "https://grow-farm.com",
    ],
    contactPoint: {
      "@type": "ContactPoint",
      contactType: "customer service",
      availableLanguage: "Korean",
      url: "https://grow-farm.com/suggest",
    },
  },

  // 게시글 구조화 데이터
  article: (title: string, content: string, author: string, publishedTime: string, url: string, modifiedTime?: string) => ({
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
    dateModified: modifiedTime || publishedTime,
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
    inLanguage: "ko-KR",
    articleSection: "커뮤니티",
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

  // FAQ 구조화 데이터
  faq: (questions: Array<{ question: string; answer: string }>) => ({
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: questions.map(item => ({
      "@type": "Question",
      name: item.question,
      acceptedAnswer: {
        "@type": "Answer",
        text: item.answer,
      },
    })),
  }),

  // 사용자 프로필 구조화 데이터
  person: (name: string, description?: string, image?: string, url?: string) => ({
    "@context": "https://schema.org",
    "@type": "Person",
    name: name,
    description: description,
    image: image || "https://grow-farm.com/log.png",
    url: url || `https://grow-farm.com/user/${encodeURIComponent(name)}`,
    memberOf: {
      "@type": "Organization",
      name: "비밀로그",
    },
  }),

  // 검색 결과 페이지 구조화 데이터
  searchResultsPage: (query: string, resultsCount: number) => ({
    "@context": "https://schema.org",
    "@type": "SearchResultsPage",
    name: `"${query}" 검색 결과`,
    description: `"${query}"에 대한 ${resultsCount}개의 검색 결과`,
    url: `https://grow-farm.com/board?q=${encodeURIComponent(query)}`,
    mainEntity: {
      "@type": "ItemList",
      numberOfItems: resultsCount,
    },
  }),

  // 이벤트 구조화 데이터 (롤링페이퍼용)
  event: (name: string, description: string, startDate: string, url: string) => ({
    "@context": "https://schema.org",
    "@type": "Event",
    name: name,
    description: description,
    startDate: startDate,
    url: url,
    location: {
      "@type": "VirtualLocation",
      url: url,
    },
    organizer: {
      "@type": "Organization",
      name: "비밀로그",
      url: "https://grow-farm.com",
    },
    eventAttendanceMode: "https://schema.org/OnlineEventAttendanceMode",
    eventStatus: "https://schema.org/EventScheduled",
  }),

  // 웹 애플리케이션 구조화 데이터
  webApplication: {
    "@context": "https://schema.org",
    "@type": "WebApplication",
    name: "비밀로그",
    description: "익명 롤링페이퍼와 커뮤니티 기능을 제공하는 소셜 네트워킹 웹 애플리케이션",
    url: "https://grow-farm.com",
    applicationCategory: "SocialNetworkingApplication",
    operatingSystem: "Web",
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "KRW",
    },
    browserRequirements: "최신 브라우저 권장",
    softwareVersion: "2.0",
    permissions: "notifications",
    featureList: [
      "익명 롤링페이퍼 작성",
      "커뮤니티 게시판",
      "실시간 알림",
      "카카오톡 로그인",
    ],
  },
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

// 메타데이터 유효성 검증 함수
export const validateMetadata = {
  // 타이틀 유효성 검증 (최대 60자 권장)
  title: (title: string): { valid: boolean; warning?: string } => {
    if (!title) return { valid: false, warning: "타이틀이 비어있습니다." };
    if (title.length > 60) return { valid: true, warning: `타이틀이 너무 깁니다 (${title.length}자). 60자 이하를 권장합니다.` };
    if (title.length < 10) return { valid: true, warning: `타이틀이 너무 짧습니다 (${title.length}자). 10자 이상을 권장합니다.` };
    return { valid: true };
  },

  // 설명 유효성 검증 (최대 160자 권장)
  description: (description: string): { valid: boolean; warning?: string } => {
    if (!description) return { valid: false, warning: "설명이 비어있습니다." };
    if (description.length > 160) return { valid: true, warning: `설명이 너무 깁니다 (${description.length}자). 160자 이하를 권장합니다.` };
    if (description.length < 50) return { valid: true, warning: `설명이 너무 짧습니다 (${description.length}자). 50자 이상을 권장합니다.` };
    return { valid: true };
  },

  // URL 유효성 검증
  url: (url: string): { valid: boolean; warning?: string } => {
    try {
      new URL(url);
      if (url.length > 2048) return { valid: true, warning: "URL이 너무 깁니다. 2048자 이하를 권장합니다." };
      return { valid: true };
    } catch {
      return { valid: false, warning: "유효하지 않은 URL입니다." };
    }
  },

  // 이미지 URL 유효성 검증
  imageUrl: (url: string): { valid: boolean; warning?: string } => {
    const validExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg'];
    const hasValidExtension = validExtensions.some(ext => url.toLowerCase().includes(ext));
    if (!hasValidExtension) {
      return { valid: true, warning: "이미지 확장자를 확인할 수 없습니다." };
    }
    return { valid: true };
  },
};

// HTML 태그 제거 및 텍스트 정리
export const cleanHtmlContent = (html: string): string => {
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // script 태그 제거
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '') // style 태그 제거
    .replace(/<[^>]*>/g, '') // 모든 HTML 태그 제거
    .replace(/\s+/g, ' ') // 연속된 공백을 하나로
    .trim();
};

// 동적 OG 이미지 URL 생성
export const generateDynamicOgImage = (params: {
  title: string;
  author?: string;
  type?: 'post' | 'paper' | 'default';
}): string => {
  const ogImageUrl = new URL('/api/og', 'https://grow-farm.com');
  ogImageUrl.searchParams.set('title', params.title);
  if (params.author) ogImageUrl.searchParams.set('author', params.author);
  if (params.type) ogImageUrl.searchParams.set('type', params.type);
  return ogImageUrl.toString();
};

// 페이지네이션 메타 태그 생성
export const generatePaginationMeta = (currentPage: number, totalPages: number, baseUrl: string) => {
  const meta: { rel: string; url: string }[] = [];

  if (currentPage > 1) {
    meta.push({ rel: 'prev', url: `${baseUrl}?page=${currentPage - 1}` });
  }

  if (currentPage < totalPages) {
    meta.push({ rel: 'next', url: `${baseUrl}?page=${currentPage + 1}` });
  }

  return meta;
};

// 언어별 hreflang 태그 생성 (향후 다국어 지원용)
export const generateHreflangTags = (path: string, languages: string[] = ['ko']) => {
  return languages.map(lang => ({
    hrefLang: lang,
    href: `https://grow-farm.com${path}`,
  }));
};

// 소셜 미디어 공유 URL 생성
export const generateShareUrls = {
  kakao: (url: string, title: string) => {
    // 카카오톡 공유는 SDK를 통해 처리
    return { url, title };
  },
  facebook: (url: string) => {
    return `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
  },
  twitter: (url: string, text: string) => {
    return `https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
  },
  line: (url: string, text: string) => {
    return `https://social-plugins.line.me/lineit/share?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
  },
}; 