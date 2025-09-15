"use client"

import React from "react";
import Link from "next/link";
import {
  Footer,
  FooterCopyright,
  FooterDivider,
  FooterIcon,
  FooterLink,
  FooterLinkGroup,
  FooterTitle,
} from "flowbite-react";
import { Github, Twitter, Instagram, Mail } from "lucide-react";

export const HomeFooter: React.FC = () => {
  return (
    <Footer container className="bg-white rounded-none shadow-sm">
      <div className="w-full">
        <div className="grid w-full justify-between sm:flex sm:justify-between md:flex md:grid-cols-1">
          {/* Brand Section */}
          <div>
            <Link href="/" className="flex items-center mb-4">
              <span className="text-2xl font-bold text-gray-900">비밀로그</span>
            </Link>
            <p className="text-sm text-gray-600 max-w-xs">
              익명으로 마음을 전하는 특별한 공간
            </p>
          </div>

          {/* Links Grid - All sections in one row */}
          <div className="grid grid-cols-2 gap-8 sm:mt-4 sm:grid-cols-4 sm:gap-6">
            {/* 서비스 */}
            <div>
              <FooterTitle title="서비스" className="text-gray-900 font-semibold mb-4" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/board" className="text-gray-600 hover:text-gray-900">
                  게시판
                </FooterLink>
                <FooterLink as={Link} href="/visit" className="text-gray-600 hover:text-gray-900">
                  롤링페이퍼 방문
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 고객지원 */}
            <div>
              <FooterTitle title="고객지원" className="text-gray-900 font-semibold mb-4" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/suggest" className="text-gray-600 hover:text-gray-900">
                  건의하기
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 정책 */}
            <div>
              <FooterTitle title="정책" className="text-gray-900 font-semibold mb-4" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/privacy" className="text-gray-600 hover:text-gray-900">
                  개인정보처리방침
                </FooterLink>
                <FooterLink as={Link} href="/terms" className="text-gray-600 hover:text-gray-900">
                  이용약관
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 운영 */}
            <div>
              <FooterTitle title="운영" className="text-gray-900 font-semibold mb-4" />
              <FooterLinkGroup col>
                <FooterLink
                  href="https://jaeiktech.tistory.com"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-600 hover:text-gray-900"
                >
                  개발자 블로그
                </FooterLink>
              </FooterLinkGroup>
            </div>
          </div>
        </div>

        <FooterDivider className="border-gray-200 my-8" />

        {/* Bottom Section with Copyright and Social Icons */}
        <div className="w-full sm:flex sm:items-center sm:justify-between">
          <FooterCopyright
            by="비밀로그"
            year={2025}
            className="text-gray-500 text-sm"
          />

          {/* Social Media Icons */}
          <div className="mt-4 flex space-x-6 sm:mt-0 sm:justify-center">
            <FooterIcon
              href="https://github.com"
              icon={() => (
                <Github className="w-5 h-5 text-gray-500 hover:text-gray-900 transition-colors" />
              )}
              className="hover:bg-gray-100 rounded-lg"
            />
            <FooterIcon
              href="https://twitter.com"
              icon={() => (
                <Twitter className="w-5 h-5 text-gray-500 hover:text-gray-900 transition-colors" />
              )}
              className="hover:bg-gray-100 rounded-lg"
            />
            <FooterIcon
              href="https://instagram.com"
              icon={() => (
                <Instagram className="w-5 h-5 text-gray-500 hover:text-gray-900 transition-colors" />
              )}
              className="hover:bg-gray-100 rounded-lg"
            />
            <FooterIcon
              href="mailto:contact@grow-farm.com"
              icon={() => (
                <Mail className="w-5 h-5 text-gray-500 hover:text-gray-900 transition-colors" />
              )}
              className="hover:bg-gray-100 rounded-lg"
            />
          </div>
        </div>
      </div>
    </Footer>
  );
};
