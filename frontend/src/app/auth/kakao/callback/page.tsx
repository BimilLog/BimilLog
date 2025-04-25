"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import useAuthStore from "@/util/authStore";

// 실제 콘텐츠 분리 - Suspense로 감싸기 위한 컴포넌트
function KakaoCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<string>("처리 중...");
  const { checkAuth } = useAuthStore();

  useEffect(() => {
    const processKakaoLogin = async () => {
      // URL에서 코드 파라미터 추출
      const code = searchParams.get("code");
      if (!code) {
        setStatus("인증 코드가 없습니다. 다시 로그인해주세요.");
        return;
      }

      try {
        setStatus("카카오 로그인 정보를 처리 중입니다...");

        // 백엔드 서버로 코드 전송
        const response = await fetch(
          `http://localhost:8080/auth/login?code=${code}`,
          {
            method: "GET",
            credentials: "include", // 쿠키를 포함하여 요청
          }
        );

        if (!response.ok) {
          throw new Error("로그인 처리 중 오류가 발생했습니다.");
        }

        // 응답 처리
        try {
          // 응답에 내용이 있는지 확인
          const text = await response.text();

          // 응답이 비어있으면 기존 회원
          if (!text) {
            await handleExistingUser();
            return;
          }

          // 응답이 있으면 JSON으로 파싱 시도
          const data = text ? JSON.parse(text) : null;

          if (typeof data === "number") {
            // 신규 회원 - 농장 이름 설정 페이지로 이동
            router.push(`/auth/signup?tokenId=${data}`);
          } else {
            // 예상치 못한 응답 형식이지만 로그인 처리
            await handleExistingUser();
          }
        } catch (jsonError) {
          // JSON 파싱 오류는 기존 회원으로 처리
          console.error("응답 처리 오류, 기존 회원으로 처리:", jsonError);
          await handleExistingUser();
        }
      } catch (error) {
        console.error("카카오 로그인 처리 오류:", error);
        setStatus("로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    };

    // 기존 회원 처리 함수
    const handleExistingUser = async () => {
      setStatus("로그인 성공! 홈페이지로 이동합니다...");
      await checkAuth(); // 인증 상태 업데이트
      router.push("/");
    };

    processKakaoLogin();
  }, [searchParams, router, checkAuth]);

  return (
    <div className="card p-4 text-center">
      <h3 className="mb-4">카카오 로그인</h3>
      <div className="d-flex justify-content-center mb-3">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">로딩중...</span>
        </div>
      </div>
      <p className="mb-0">{status}</p>
    </div>
  );
}

// 로딩 중 컴포넌트
function LoadingCard() {
  return (
    <div className="card p-4 text-center">
      <h3 className="mb-4">카카오 로그인</h3>
      <div className="d-flex justify-content-center mb-3">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">초기화 중...</span>
        </div>
      </div>
      <p className="mb-0">준비 중...</p>
    </div>
  );
}

// 메인 페이지 컴포넌트
export default function KakaoCallbackPage() {
  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row justify-content-center">
          <div className="col-lg-6">
            <Suspense fallback={<LoadingCard />}>
              <KakaoCallbackContent />
            </Suspense>
          </div>
        </div>
      </div>
    </main>
  );
}
