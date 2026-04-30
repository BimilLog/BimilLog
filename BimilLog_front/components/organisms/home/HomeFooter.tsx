"use client"

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/common/useAuth";
import {
  Footer,
  FooterCopyright,
  FooterDivider,
  FooterLink,
  FooterLinkGroup,
  FooterTitle,
} from "flowbite-react";
import { Github, Contact, ExternalLink } from "lucide-react";

export const HomeFooter: React.FC = React.memo(() => {
  const router = useRouter();
  const { isAuthenticated } = useAuth({ skipRefresh: true });

  const handleFriendClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      router.push('/login?redirect=/friends');
    } else {
      router.push('/friends');
    }
  };

  return (
    <Footer container className="bg-paper-soft dark:bg-paper-50/95 rounded-none shadow-sm border-t border-ink-soft dark:border-stamp-red/15">
      <div className="w-full">
        <div className="w-full space-y-8">
          {/* Brand Section */}
          <div>
            <Link href="/" className="flex items-center gap-2 mb-4">
              <span aria-hidden="true" className="inline-block w-2 h-2 rounded-full bg-stamp-red shadow-[0_0_0_3px_rgba(199,62,62,0.18)]" />
              <span className="font-display text-2xl font-bold text-ink dark:text-foreground">비밀로그</span>
            </Link>
            <p className="text-sm text-ink-soft dark:text-muted-foreground max-w-xs">
              익명으로 마음을 전하는 종이 한 장의 공간
            </p>
          </div>

          <hr data-testid="footer-divider" className="w-full sm:mx-auto lg:my-6 border-border my-6" />

          {/* Links Grid - 반응형 (모바일 2열 → 태블릿 3열 → 데스크톱 5열) */}
          <div
            data-testid="home-footer-grid"
            className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-6"
          >
            {/* 서비스 */}
            <div>
              <FooterTitle title="서비스" className="text-foreground font-semibold mb-4 whitespace-nowrap" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/rolling-paper" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  롤링페이퍼
                </FooterLink>
                <FooterLink as={Link} href="/visit" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  롤링페이퍼 방문
                </FooterLink>
                <FooterLink as={Link} href="/board" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  게시판
                </FooterLink>
                <FooterLink as={Link} href="/board/write" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  게시글 작성
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 회원 */}
            <div>
              <FooterTitle title="회원" className="text-foreground font-semibold mb-4 whitespace-nowrap" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/login" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  로그인
                </FooterLink>
                <FooterLink as={Link} href="/mypage" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  마이페이지
                </FooterLink>
                <FooterLink as={Link} href="/settings" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  설정
                </FooterLink>
                <FooterLink
                  href="#"
                  onClick={handleFriendClick}
                  className="text-muted-foreground hover:text-foreground cursor-pointer whitespace-nowrap"
                >
                  친구
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 지원 */}
            <div>
              <FooterTitle title="지원" className="text-foreground font-semibold mb-4 whitespace-nowrap" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/suggest" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  건의하기
                </FooterLink>
                <FooterLink as={Link} href="/install" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  설치 가이드
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 정책 */}
            <div>
              <FooterTitle title="정책" className="text-foreground font-semibold mb-4 whitespace-nowrap" />
              <FooterLinkGroup col>
                <FooterLink as={Link} href="/privacy" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  개인정보처리방침
                </FooterLink>
                <FooterLink as={Link} href="/terms" className="text-muted-foreground hover:text-foreground whitespace-nowrap">
                  이용약관
                </FooterLink>
              </FooterLinkGroup>
            </div>

            {/* 킬링타임 */}
            <div>
              <FooterTitle title="킬링타임" className="text-foreground font-semibold mb-4 whitespace-nowrap" />
              <FooterLinkGroup col>
                <FooterLink
                  href="https://liketests.vercel.app/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-muted-foreground hover:text-foreground whitespace-nowrap inline-flex items-center gap-1"
                >
                  심리테스트
                  <ExternalLink className="w-3 h-3" aria-hidden="true" />
                </FooterLink>
                <FooterLink
                  href="https://v0-drum-machine-with-claude.vercel.app/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-muted-foreground hover:text-foreground whitespace-nowrap inline-flex items-center gap-1"
                >
                  비트 만들기
                  <ExternalLink className="w-3 h-3" aria-hidden="true" />
                </FooterLink>
              </FooterLinkGroup>
            </div>
          </div>
        </div>

        <FooterDivider className="border-border my-8" />

        {/* Bottom Section with Copyright and Social Icons */}
        <div className="w-full sm:flex sm:items-center sm:justify-between">
          <FooterCopyright
            by="비밀로그"
            year={2025}
            className="text-muted-foreground text-sm"
          />

          {/* Social Media Icons */}
          <div className="mt-4 flex space-x-6 sm:mt-0 sm:justify-center">
            <a
              href="https://github.com/BimilLog"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="비밀로그 GitHub 저장소"
              className="hover:bg-accent rounded-lg p-2 transition-colors"
            >
              <Github className="w-5 h-5 text-foreground" />
            </a>
            <a
              href="https://jaeiktech.tistory.com"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="개발자 블로그"
              className="hover:bg-accent rounded-lg p-2 transition-colors"
            >
              <Contact className="w-5 h-5 text-foreground" />
            </a>
          </div>
        </div>
      </div>
    </Footer>
  );
});

HomeFooter.displayName = "HomeFooter";
