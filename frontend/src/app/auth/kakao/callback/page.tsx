"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function KakaoCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<string>("처리 중...");

  useEffect(() => {
    const processKakaoLogin = async () => {
      try {
        // URL에서 코드 파라미터 추출
        const code = searchParams.get("code");

        if (!code) {
          setStatus("인증 코드가 없습니다. 다시 로그인해주세요.");
          return;
        }

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

        const data = await response.json();
        if (typeof data === "number") {
          // 신규 회원 - 농장 이름 설정 페이지로 이동
          router.push(`/auth/signup?tokenId=${data}`);
        } else {
          // 기존 회원 - 메인 페이지로 이동
          setStatus("로그인 성공! 홈페이지로 이동합니다...");
          router.push("/");
        }
      } catch (error) {
        console.error("카카오 로그인 처리 오류:", error);
        setStatus("로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    };

    processKakaoLogin();
  }, [searchParams, router]);

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row justify-content-center">
          <div className="col-lg-6">
            <div className="card p-4 text-center">
              <h3 className="mb-4">카카오 로그인</h3>
              <div className="d-flex justify-content-center mb-3">
                <div className="spinner-border text-primary" role="status">
                  <span className="visually-hidden">로딩중...</span>
                </div>
              </div>
              <p className="mb-0">{status}</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
