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
    <footer className="bg-gray-800 text-white py-8 sm:py-12">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* 로고 및 소개 */}
        <div className="mb-8">
          <div className="flex items-end mb-2">
            <h2 className="text-xl sm:text-2xl font-bold text-white leading-none whitespace-nowrap">비밀로그</h2>
            <span className="ml-3 text-xs text-gray-400 leading-none">v2.0.0</span>
          </div>
          <p className="text-gray-300 text-sm sm:text-base leading-relaxed">익명으로 마음을 전하는 특별한 공간</p>
        </div>

        {/* 링크 섹션들 - 가로 4개 컬럼 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {/* 고객지원 */}
          <div>
            <h3 className="font-semibold text-sm sm:text-base text-white mb-3">고객지원</h3>
            <ul className="space-y-2">
              <li>
                <Link
                  href="/suggest"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  건의하기
                </Link>
              </li>
            </ul>
          </div>

          {/* 운영 */}
          <div>
            <h3 className="font-semibold text-sm sm:text-base text-white mb-3">운영</h3>
            <ul className="space-y-2">
              <li>
                <a
                  href="https://jaeiktech.tistory.com"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  개발자 블로그
                </a>
              </li>
            </ul>
          </div>

          {/* 서비스 */}
          <div>
            <h3 className="font-semibold text-sm sm:text-base text-white mb-3">서비스</h3>
            <ul className="space-y-2">
              <li>
                <Link
                  href="/board"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  게시판
                </Link>
              </li>
              <li>
                <Link
                  href="/visit"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  롤링페이퍼 방문
                </Link>
              </li>
            </ul>
          </div>

          {/* 정책 */}
          <div>
            <h3 className="font-semibold text-sm sm:text-base text-white mb-3">정책</h3>
            <ul className="space-y-2">
              <li>
                <Link
                  href="/privacy"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  개인정보처리방침
                </Link>
              </li>
              <li>
                <Link
                  href="/terms"
                  className="text-gray-300 hover:text-white transition-all text-sm sm:text-base"
                >
                  이용약관
                </Link>
              </li>
            </ul>
          </div>
        </div>

        {/* 저작권 정보 */}
        <div className="border-t border-gray-600 mt-6 sm:mt-8 pt-6 sm:pt-8 text-center">
          <div className="flex flex-col sm:flex-row justify-center items-center gap-2 sm:gap-4">
            <p className="text-gray-300 text-sm">&copy; 2025 비밀로그. All rights reserved.</p>
          </div>
        </div>
      </div>
    </footer>
  );
};
