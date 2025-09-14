import React from "react";
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
          href: "https://jaeiktech.tistory.com",
          label: "개발자 블로그",
          external: true,
        },
      ],
    },
  ];

  return (
    <footer className="bg-gradient-to-br from-purple-900 via-indigo-900 to-pink-900 text-white py-8 sm:py-12">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-6 sm:gap-8">
          {/* 로고 및 소개 */}
          <div className="col-span-2 sm:col-span-3 lg:col-span-1">
            <div className="flex items-center mb-4">
              <h2 className="text-xl sm:text-2xl font-bold text-white">비밀로그</h2>
              <span className="ml-2 text-xs text-purple-200">v2.0.0</span>
            </div>
            <p className="text-purple-100 text-sm sm:text-base leading-relaxed">익명으로 마음을 전하는 특별한 공간</p>
          </div>

          {/* 링크 섹션들 */}
          {footerSections.map((section, index) => (
            <div key={index}>
              <h3 className="font-semibold mb-3 sm:mb-4 text-sm sm:text-base text-white">{section.title}</h3>
              <ul className="space-y-1 sm:space-y-2">
                {section.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    {link.external ? (
                      <a
                        href={link.href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="block py-2 -mx-2 px-2 text-purple-100 hover:text-white hover:bg-white/10 rounded-lg transition-all text-sm sm:text-base min-h-[44px] flex items-center touch-manipulation"
                      >
                        {link.label}
                      </a>
                    ) : (
                      <Link
                        href={link.href}
                        className="block py-2 -mx-2 px-2 text-purple-100 hover:text-white hover:bg-white/10 rounded-lg transition-all text-sm sm:text-base min-h-[44px] flex items-center touch-manipulation"
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
        <div className="border-t border-purple-700/30 mt-6 sm:mt-8 pt-6 sm:pt-8 text-center">
          <div className="flex flex-col sm:flex-row justify-center items-center gap-2 sm:gap-4">
            <p className="text-purple-100 text-sm">&copy; 2025 비밀로그. All rights reserved.</p>
          </div>
        </div>
      </div>
    </footer>
  );
};
