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
import { Github, Contact, Brain, Music } from "lucide-react";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

export const HomeFooter: React.FC = () => {
  const router = useRouter();
  const { isAuthenticated } = useAuth({ skipRefresh: true });
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleFriendClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      router.push('/login?redirect=/friends');
    } else {
      router.push('/friends');
    }
  };

  const handlePsychologyTest = async (e: React.MouseEvent) => {
    e.preventDefault();
    const confirmed = await confirm({
      title: "외부 사이트 이동",
      message: "개발자가 만든 킬링타임용 심리테스트 사이트로 이동됩니다.",
      confirmText: "이동",
      cancelText: "취소",
      confirmButtonVariant: "default",
      icon: <Brain className="h-8 w-8 stroke-purple-600 fill-purple-100" />
    });

    if (confirmed) {
      window.open('https://liketests.vercel.app/', '_blank', 'noopener,noreferrer');
    }
  };

  const handleBeatMaker = async (e: React.MouseEvent) => {
    e.preventDefault();
    const confirmed = await confirm({
      title: "외부 사이트 이동",
      message: "개발자가 만든 비트 만들기 사이트로 이동됩니다.",
      confirmText: "이동",
      cancelText: "취소",
      confirmButtonVariant: "default",
      icon: <Music className="h-8 w-8 stroke-purple-600 fill-purple-100" />
    });

    if (confirmed) {
      window.open('https://v0-drum-machine-with-claude.vercel.app/', '_blank', 'noopener,noreferrer');
    }
  };

  return (
    <Footer container className="bg-background rounded-none shadow-sm border-t border-border">
      <div className="w-full">
        <div className="w-full space-y-8">
          {/* Brand Section */}
          <div>
            <Link href="/" className="flex items-center mb-4">
              <span className="text-2xl font-bold text-foreground">비밀로그</span>
            </Link>
            <p className="text-sm text-muted-foreground max-w-xs">
              익명으로 마음을 전하는 특별한 공간
            </p>
          </div>

          <hr data-testid="footer-divider" className="w-full sm:mx-auto lg:my-6 border-border my-6" />

          {/* Links Grid - All sections in one row */}
          <div className="grid grid-cols-5 gap-4 sm:gap-6">
            {/* 서비스 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="서비스" className="text-foreground font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/rolling-paper" className="text-muted-foreground hover:text-foreground">
                    롤링페이퍼
                  </FooterLink>
                  <FooterLink as={Link} href="/visit" className="text-muted-foreground hover:text-foreground">
                    롤링페이퍼 방문
                  </FooterLink>
                  <FooterLink as={Link} href="/board" className="text-muted-foreground hover:text-foreground">
                    게시판
                  </FooterLink>
                  <FooterLink as={Link} href="/board/write" className="text-muted-foreground hover:text-foreground">
                    게시글 작성
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 회원 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="회원" className="text-foreground font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/login" className="text-muted-foreground hover:text-foreground">
                    로그인
                  </FooterLink>
                  <FooterLink as={Link} href="/mypage" className="text-muted-foreground hover:text-foreground">
                    마이페이지
                  </FooterLink>
                  <FooterLink as={Link} href="/settings" className="text-muted-foreground hover:text-foreground">
                    설정
                  </FooterLink>
                  <FooterLink
                    href="#"
                    onClick={handleFriendClick}
                    className="text-muted-foreground hover:text-foreground cursor-pointer"
                  >
                    친구
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 지원 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="지원" className="text-foreground font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/suggest" className="text-muted-foreground hover:text-foreground">
                    건의하기
                  </FooterLink>
                  <FooterLink as={Link} href="/install" className="text-muted-foreground hover:text-foreground">
                    설치 가이드
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 정책 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="정책" className="text-foreground font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/privacy" className="text-muted-foreground hover:text-foreground">
                    개인정보처리방침
                  </FooterLink>
                  <FooterLink as={Link} href="/terms" className="text-muted-foreground hover:text-foreground">
                    이용약관
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 킬링타임 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="킬링타임" className="text-foreground font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink
                    href="#"
                    onClick={handlePsychologyTest}
                    className="text-muted-foreground hover:text-foreground cursor-pointer"
                  >
                    심리테스트
                  </FooterLink>
                  <FooterLink
                    href="#"
                    onClick={handleBeatMaker}
                    className="text-muted-foreground hover:text-foreground cursor-pointer"
                  >
                    비트 만들기
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>
          </div>
        </div>

        <FooterDivider className="border-border my-8" />

        {/* Bottom Section with Copyright and Social Icons */}
        <div className="w-full sm:flex sm:items-center sm:justify-between">
          <FooterCopyright
            by="비밀로그 v2.3.0"
            year={2025}
            className="text-muted-foreground text-sm"
          />

          {/* Social Media Icons */}
          <div className="mt-4 flex space-x-6 sm:mt-0 sm:justify-center">
            <a
              href="https://github.com/BimilLog"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:bg-accent rounded-lg p-2 transition-colors"
            >
              <Github className="w-5 h-5 text-foreground" />
            </a>
            <a
              href="https://jaeiktech.tistory.com"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:bg-accent rounded-lg p-2 transition-colors"
            >
              <Contact className="w-5 h-5 text-foreground" />
            </a>
          </div>
        </div>
      </div>
      <ConfirmModalComponent />
    </Footer>
  );
};
