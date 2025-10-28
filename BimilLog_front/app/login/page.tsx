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

        <h5 className="mb-4 text-xl font-medium text-center text-gray-900 dark:text-white">비밀로그 시작하기</h5>
        <p className="mb-6 text-center text-gray-500 dark:text-gray-400">나만의 롤링페이퍼를 만들어 보세요</p>

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
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">나만의 롤링페이퍼 만들기 가능</span>
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
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">자주가는 롤링페이퍼의 북마크 기능</span>
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
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">마이페이지 이용 가능</span>
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
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">실시간 알림 받기 가능</span>
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
            <span className="text-base font-normal leading-tight text-gray-500 dark:text-gray-400">글과 댓글 추천 가능</span>
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
