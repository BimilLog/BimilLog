import { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  const baseUrl = 'https://grow-farm.com'

  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: [
          '/admin/',
          '/api/',
          '/_next/',
          '/mypage/settings',
          '/auth/callback',
          '/*.json$',
          '/sw.js',
          '/firebase-messaging-sw.js',
          '/board/post/*/edit',
          '/board/write',
          '/suggest/write',
        ],
        crawlDelay: 1,
      },
      {
        userAgent: 'Googlebot',
        allow: '/',
        disallow: [
          '/admin/',
          '/api/',
          '/_next/',
          '/mypage/settings',
          '/auth/callback',
        ],
      },
      {
        userAgent: 'Yeti',
        allow: '/',
        disallow: [
          '/admin/',
          '/api/',
          '/_next/',
          '/mypage/settings',
          '/auth/callback',
        ],
      },
      {
        userAgent: 'bingbot',
        allow: '/',
        disallow: [
          '/admin/',
          '/api/',
          '/_next/',
          '/mypage/settings',
          '/auth/callback',
        ],
        crawlDelay: 2,
      },
      {
        userAgent: 'facebookexternalhit',
        allow: ['/board/post/', '/rolling-paper/'],
        disallow: ['/admin/', '/api/', '/_next/'],
      },
      {
        userAgent: 'kakaotalk-scrap',
        allow: ['/board/post/', '/rolling-paper/'],
        disallow: ['/admin/', '/api/', '/_next/'],
      },
      {
        userAgent: 'AdsBot-Google',
        disallow: '/',
      },
    ],
    sitemap: [
      `${baseUrl}/sitemap.xml`,
      `${baseUrl}/sitemap-posts.xml`,
      `${baseUrl}/sitemap-papers.xml`,
    ],
    host: baseUrl,
  }
} 