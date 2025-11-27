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
import { Github, Contact, Brain } from "lucide-react";
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

  return (
    <Footer container className="bg-white rounded-none shadow-sm">
      <div className="w-full">
        <div className="w-full space-y-8">
          {/* Brand Section */}
          <div>
            <Link href="/" className="flex items-center mb-4">
              <span className="text-2xl font-bold text-gray-900">비밀로그</span>
            </Link>
            <p className="text-sm text-gray-600 max-w-xs">
              익명으로 마음을 전하는 특별한 공간
            </p>
          </div>

          <hr data-testid="footer-divider" className="w-full sm:mx-auto lg:my-6 dark:border-gray-700 border-gray-200 my-6" />

          {/* Links Grid - All sections in one row */}
          <div className="grid grid-cols-5 gap-4 sm:gap-6">
            {/* 서비스 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="서비스" className="text-gray-900 font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/rolling-paper" className="text-gray-600 hover:text-gray-900">
                    롤링페이퍼
                  </FooterLink>
                  <FooterLink as={Link} href="/visit" className="text-gray-600 hover:text-gray-900">
                    롤링페이퍼 방문
                  </FooterLink>
                  <FooterLink as={Link} href="/board" className="text-gray-600 hover:text-gray-900">
                    게시판
                  </FooterLink>
                  <FooterLink as={Link} href="/board/write" className="text-gray-600 hover:text-gray-900">
                    게시글 작성
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 회원 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="회원" className="text-gray-900 font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/login" className="text-gray-600 hover:text-gray-900">
                    로그인
                  </FooterLink>
                  <FooterLink as={Link} href="/mypage" className="text-gray-600 hover:text-gray-900">
                    마이페이지
                  </FooterLink>
                  <FooterLink as={Link} href="/settings" className="text-gray-600 hover:text-gray-900">
                    설정
                  </FooterLink>
                  <FooterLink
                    href="#"
                    onClick={handleFriendClick}
                    className="text-gray-600 hover:text-gray-900 cursor-pointer"
                  >
                    친구
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 지원 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="지원" className="text-gray-900 font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink as={Link} href="/suggest" className="text-gray-600 hover:text-gray-900">
                    건의하기
                  </FooterLink>
                  <FooterLink as={Link} href="/install" className="text-gray-600 hover:text-gray-900">
                    설치 가이드
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>

            {/* 정책 */}
            <div className="flex justify-center">
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
            </div>

            {/* 킬링타임 */}
            <div className="flex justify-center">
              <div>
                <FooterTitle title="킬링타임" className="text-gray-900 font-semibold mb-4" />
                <FooterLinkGroup col>
                  <FooterLink
                    href="#"
                    onClick={handlePsychologyTest}
                    className="text-gray-600 hover:text-gray-900 cursor-pointer"
                  >
                    심리테스트
                  </FooterLink>
                </FooterLinkGroup>
              </div>
            </div>
          </div>
        </div>

        <FooterDivider className="border-gray-200 my-8" />

        {/* Bottom Section with Copyright and Social Icons */}
        <div className="w-full sm:flex sm:items-center sm:justify-between">
          <FooterCopyright
            by="비밀로그 v2.2.0"
            year={2025}
            className="text-gray-500 text-sm"
          />

          {/* Social Media Icons */}
          <div className="mt-4 flex space-x-6 sm:mt-0 sm:justify-center">
            <a
              href="https://github.com/BimilLog"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:bg-gray-100 rounded-lg p-2"
            >
              <Github className="w-5 h-5 stroke-gray-800 fill-gray-100 hover:stroke-gray-900 hover:fill-gray-200 transition-colors" />
            </a>
            <a
              href="https://jaeiktech.tistory.com"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:bg-gray-100 rounded-lg p-2"
            >
              <Contact className="w-5 h-5 stroke-slate-600 fill-slate-100 hover:stroke-slate-900 hover:fill-slate-200 transition-colors" />
            </a>
          </div>
        </div>
      </div>
      <ConfirmModalComponent />
    </Footer>
  );
};
