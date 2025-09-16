import { Metadata } from "next";
import { notFound } from "next/navigation";
import { apiClient, type Post } from "@/lib/api";
import { generateStructuredData, generateKeywords } from "@/lib/seo";
import { PostDetailClient } from "@/components/organisms/board";

interface Props {
  params: Promise<{
    id: string;
  }>;
}

// 동적 메타데이터 생성
export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id: postId } = await params;

  try {
    // 서버사이드에서 게시글 데이터 조회하여 SEO 메타데이터 동적 생성
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/post/${postId}`, {
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error('Failed to fetch post');
    }

    const post = await response.json();

    if (!post || !post.id) {
      return {
        title: "게시글을 찾을 수 없습니다 | 비밀로그",
        description: "요청하신 게시글을 찾을 수 없습니다.",
      };
    }
    // HTML 태그 제거 후 160자로 잘라서 meta description 생성
    const truncatedContent = post.content
      .replace(/<[^>]*>/g, "")
      .substring(0, 160);

    // 동적 OG 이미지 생성을 위한 API 엔드포인트 URL 구성
    const ogImageUrl = new URL(`/api/og`, "https://grow-farm.com");
    ogImageUrl.searchParams.set("title", post.title);
    ogImageUrl.searchParams.set("author", post.userName);
    ogImageUrl.searchParams.set("type", "post");

        return {
            title: `${post.title} | 비밀로그`,
            description: truncatedContent || "비밀로그 커뮤니티의 게시글입니다.",
            keywords: generateKeywords(["게시글", post.title, post.userName]),
            authors: [{ name: post.userName }],
            alternates: {
                canonical: `https://grow-farm.com/board/post/${postId}`,
            },
            openGraph: {
                title: `${post.title} | 비밀로그`,
                description: truncatedContent || "비밀로그 커뮤니티의 게시글입니다.",
                url: `https://grow-farm.com/board/post/${postId}`,
                siteName: "비밀로그",
                images: [
                    {
                        url: ogImageUrl.toString(),
                        width: 1200,
                        height: 630,
                        alt: `${post.title} - 비밀로그`,
                    },
                ],
                locale: "ko_KR",
                type: "article",
                publishedTime: post.createdAt,
                authors: [post.userName],
            },
            twitter: {
                card: "summary_large_image",
                title: `${post.title} | 비밀로그`,
                description: truncatedContent || "비밀로그 커뮤니티의 게시글입니다.",
                images: [ogImageUrl.toString()],
            },
        };
  } catch {
    return {
      title: "게시글을 찾을 수 없습니다 | 비밀로그",
      description: "요청하신 게시글을 찾을 수 없습니다.",
    };
  }
}

// 서버 컴포넌트
export default async function PostDetailPage({ params }: Props) {
  const { id: postId } = await params;

  // 서버 컴포넌트에서 초기 데이터 페칭 - 클라이언트 하이드레이션 전에 데이터 확보
  try {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/post/${postId}`, {
      cache: 'no-store'
    });

    if (!response.ok) {
      notFound();
    }

    const post = await response.json();

    if (!post || !post.id) {
      notFound();
    }

    // SEO를 위한 구조화된 데이터(JSON-LD) 생성 - 검색엔진이 콘텐츠를 이해할 수 있도록 함
    const articleJsonLd = generateStructuredData.article(
      post.title,
      post.content,
      post.userName,
      post.createdAt,
      `https://grow-farm.com/board/post/${postId}`
    );

    // Breadcrumb 구조화 데이터 추가
    const breadcrumbJsonLd = generateStructuredData.breadcrumb([
      { title: "홈", href: "/" },
      { title: "커뮤니티", href: "/board" },
      { title: post.title }
    ]);

    return (
      <>
        {/* JSON-LD 스크립트를 head에 삽입하여 구조화된 데이터 제공 */}
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(articleJsonLd) }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbJsonLd) }}
        />
        {/* 서버에서 가져온 초기 데이터를 클라이언트 컴포넌트에 전달 */}
        <PostDetailClient initialPost={post} postId={postId} />
      </>
    );
  } catch {
    notFound();
  }
}
