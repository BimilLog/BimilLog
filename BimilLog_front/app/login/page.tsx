"use client";

import { useEffect } from "react";
import { ErrorAlert, InfoAlert } from "@/components";
import { Card } from "flowbite-react";
import Image from "next/image";
import { useAuth } from "@/hooks";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";
import { AuthLoadingScreen } from "@/components";
import { useAuthError } from "@/hooks";
import { kakaoAuthManager } from "@/lib/auth/kakao";
import { naverAuthManager } from "@/lib/auth/naver";
import { googleAuthManager } from "@/lib/auth/google";

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const searchParams = useSearchParams();
  const { clearAuthError } = useAuthError();

  // URL 파라미터에서 에러 메시지를 추출 (카카오 OAuth 콜백에서 전달됨)
  const errorCode = searchParams.get("error");

  // 에러 코드별 사용자 친화적 메시지
  const getErrorMessage = (code: string | null): string => {
    if (!code) return "";

    const errorMessages: Record<string, string> = {
      "no_code": "카카오 로그인에 실패했습니다. 다시 시도해주세요.",
      "callback_failed": "로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
      "login_failed": "로그인에 실패했습니다. 다시 시도해주세요.",
      "access_denied": "카카오 로그인이 취소되었습니다.",
    };

    return errorMessages[code] || decodeURIComponent(code);
  };

  const errorMessage = getErrorMessage(errorCode);

  // 이미 로그인된 사용자는 홈페이지로 자동 리다이렉션
  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, router]);

  // 에러가 있을 경우 10초 후 자동으로 에러 상태 초기화 (사용자 경험 개선)
  useEffect(() => {
    if (errorCode) {
      const timer = setTimeout(() => {
        clearAuthError();
      }, 10000);
      return () => clearTimeout(timer);
    }
  }, [errorCode, clearAuthError]);

  const handleLogin = () => {
    kakaoAuthManager.redirectToKakaoAuth();
  };

  const handleNaverLogin = () => {
    naverAuthManager.redirectToNaverAuth();
  };

  const handleGoogleLogin = () => {
    googleAuthManager.redirectToGoogleAuth();
  };

  // 로그인 상태 확인 중일 때 로딩 스크린 표시
  if (isLoading) {
    return <AuthLoadingScreen message="로딩 중..." />;
  }

  return (
    <AuthLayout
      breadcrumbItems={[
        { title: "홈", href: "/" },
        { title: "로그인", href: "/login" },
      ]}
    >
      <Card className="max-w-sm mx-auto bg-cyan-50 dark:bg-gray-800 border-cyan-200 dark:border-gray-700">
        {/* URL 파라미터로 전달된 에러가 있을 경우에만 에러 메시지 표시 */}
        {errorMessage && (
          <div className="mb-4">
            <ErrorAlert>
              <div className="space-y-2">
                <p className="font-semibold">로그인 오류</p>
                <p className="text-sm">{errorMessage}</p>
                <button
                  onClick={handleLogin}
                  className="text-sm font-medium text-purple-600 hover:text-purple-700 underline"
                >
                  다시 로그인하기
                </button>
              </div>
            </ErrorAlert>
          </div>
        )}

        <div className="mb-6 text-center">
          <div className="inline-flex items-center justify-center rounded-2xl border border-cyan-100 bg-white/90 px-6 py-3 text-xl font-semibold text-gray-900 shadow-sm dark:border-gray-600 dark:bg-gray-700 dark:text-white">
            비밀로그 시작하기
          </div>
          <p className="mt-3 text-gray-500 dark:text-gray-400">나만의 롤링페이퍼를 만들어 보세요</p>
        </div>

        <ul className="my-7 space-y-5">
          <li className="flex space-x-3">
            <svg
              className="h-5 w-5 shrink-0 text-pink-600 dark:text-pink-500"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">롤링페이퍼 개설하기</span>
          </li>
          <li className="flex space-x-3">
            <svg
              className="h-5 w-5 shrink-0 text-pink-600 dark:text-pink-500"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">친구로부터 비밀메시지를 받기</span>
          </li>
          <li className="flex space-x-3">
            <svg
              className="h-5 w-5 shrink-0 text-pink-600 dark:text-pink-500"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">마이페이지에서 활동점수를 확인</span>
          </li>
          <li className="flex space-x-3">
            <svg
              className="h-5 w-5 shrink-0 text-pink-600 dark:text-pink-500"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">실시간 알림 받기</span>
          </li>
          <li className="flex space-x-3">
            <svg
              className="h-5 w-5 shrink-0 text-pink-600 dark:text-pink-500"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">글과 댓글에 추천</span>
          </li>
        </ul>

        <button
          onClick={handleLogin}
          className="w-full transition-all duration-200 hover:scale-[0.98] active:scale-[0.96] focus:outline-none focus:ring-4 focus:ring-cyan-200 dark:focus:ring-cyan-900 rounded-lg overflow-hidden"
          aria-label="카카오로 로그인하기"
        >
          {/* 데스크탑: wide 버전 사용 */}
          <Image
            src="/kakao_login_medium_wide.png"
            alt="카카오 로그인"
            width={300}
            height={45}
            className="hidden sm:block w-full h-auto touch-manipulation"
            priority
          />
          {/* 모바일: narrow 버전 사용 */}
          <Image
            src="/kakao_login_medium_wide.png"
            alt="카카오 로그인"
            width={183}
            height={45}
            className="sm:hidden w-full h-auto touch-manipulation"
            priority
          />
        </button>

        {/* 네이버 로그인 버튼 */}
        {/* 네이버 로그인 버튼 */}
        <button
          onClick={handleNaverLogin}
          className="w-full transition-all duration-200 hover:scale-[0.98] active:scale-[0.96]
                     focus:outline-none focus:ring-4 focus:ring-green-200 dark:focus:ring-green-900
                     rounded-lg overflow-hidden origin-center"
          aria-label="네이버로 로그인하기"
          style={{ transform: 'scaleY(0.95)', transformOrigin: 'center' }} // ✅ 버튼 자체도 세로 줄임
        >
          <Image
            src="/naver_login_button.png"
            alt="네이버 로그인"
            width={300}
            height={45}
            className="hidden sm:block w-full h-auto touch-manipulation"
            priority
          />
          <Image
            src="/naver_login_button.png"
            alt="네이버 로그인"
            width={183}
            height={45}
            className="sm:hidden w-full h-auto touch-manipulation"
            priority
          />
        </button>

        {/* 구글 로그인 버튼 */}
        <button
          onClick={handleGoogleLogin}
          aria-label="구글 계정으로 로그인"
          className="mt-2 flex w-full items-center justify-center gap-3 rounded-lg border border-[#dadce0] bg-white px-4 py-3 text-sm font-semibold text-[#3c4043] transition hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1a73e8] dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:hover:bg-gray-700"
        >
          <span className="flex h-6 w-6 items-center justify-center rounded-md bg-transparent">
            <Image
              src="/icons/google-g.svg"
              alt="Google G 로고"
              width={20}
              height={20}
              priority
            />
          </span>
          <span className="tracking-tight">구글 계정으로 로그인</span>
        </button>

        <div className="mt-6">
          <InfoAlert icon={false}>
            <div>
              <p className="font-semibold mb-2">로그인 없이도 이용 가능!</p>
              <p className="text-sm mb-1">
                로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수 있고 게시판 이용이 가능합니다.
              </p>
            </div>
          </InfoAlert>
        </div>
      </Card>
    </AuthLayout>
  );
}
