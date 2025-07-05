"use client";

import Link from "next/link";

interface FooterLink {
  href: string;
  label: string;
  external?: boolean;
}

interface FooterSection {
  title: string;
  links: FooterLink[];
}

export const HomeFooter: React.FC = () => {
  const footerSections: FooterSection[] = [
    {
      title: "서비스",
      links: [
        { href: "/board", label: "게시판" },
        { href: "/visit", label: "롤링페이퍼 방문" },
      ],
    },
    {
      title: "고객지원",
      links: [{ href: "/suggest", label: "건의하기" }],
    },
    {
      title: "정책",
      links: [
        { href: "/privacy", label: "개인정보처리방침" },
        { href: "/terms", label: "이용약관" },
      ],
    },
    {
      title: "운영",
      links: [
        {
          href: "https://cyclic-icebreaker-daa.notion.site/1d4a9f47800c80a1b12fc2aae7befd0e",
          label: "개발자 노션",
          external: true,
        },
      ],
    },
  ];

  return (
    <footer className="bg-gray-900 text-white py-12">
      <div className="container mx-auto px-4">
        <div className="grid md:grid-cols-5 gap-8">
          {/* 로고 및 소개 */}
          <div>
            <div className="flex items-center mb-4">
              <h2 className="text-2xl font-bold text-white">비밀로그</h2>
            </div>
            <p className="text-gray-400">익명으로 마음을 전하는 특별한 공간</p>
          </div>

          {/* 링크 섹션들 */}
          {footerSections.map((section, index) => (
            <div key={index}>
              <h3 className="font-semibold mb-4">{section.title}</h3>
              <ul className="space-y-2 text-gray-400">
                {section.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    {link.external ? (
                      <a
                        href={link.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="hover:text-white transition-colors"
                      >
                        {link.label}
                      </a>
                    ) : (
                      <Link
                        href={link.href}
                        className="hover:text-white transition-colors"
                      >
                        {link.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* 저작권 정보 */}
        <div className="border-t border-gray-800 mt-8 pt-8 text-center text-gray-400">
          <div className="flex flex-col sm:flex-row justify-center items-center gap-2 sm:gap-4">
            <p>&copy; 2025 비밀로그. All rights reserved.</p>
            <span className="text-xs text-gray-500">v1.0.12</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
