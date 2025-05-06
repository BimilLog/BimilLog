"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState, useEffect, Suspense } from "react";
import useAuthStore from "@/util/authStore";
import LoadingSpinner from "@/components/LoadingSpinner";
import fetchClient from "@/util/fetchClient";

const API_BASE = "https://grow-farm.com/api";

// 실제 컨텐츠 컴포넌트
function SignupContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { checkAuth } = useAuthStore();
  const [tokenId, setTokenId] = useState<number | null>(null);
  const [farmName, setFarmName] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);

  useEffect(() => {
    // 토큰 ID 검증
    const tokenIdParam = searchParams.get("tokenId");
    if (!tokenIdParam) {
      setError("토큰 정보가 없습니다. 다시 로그인해주세요.");
      setTimeout(() => router.replace("/"), 2000);
      return;
    }
    setTokenId(parseInt(tokenIdParam, 10));
  }, [searchParams, router]);

  // 농장 이름 유효성 검사
  const validateFarmName = (name: string): boolean => {
    const trimmedName = name.trim();

    if (trimmedName.length > 8) {
      setNameError("농장 이름은 8글자 이하여야 합니다.");
      return false;
    }

    setNameError(null);
    return true;
  };

  // 폼 제출 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 유효성 검사
    if (!validateFarmName(farmName) || !tokenId) {
      if (!tokenId) setError("토큰 정보가 없습니다. 다시 로그인해주세요.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // 회원가입 요청 전송
      const response = await fetchClient(`${API_BASE}/auth/signUp`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ tokenId, farmName: farmName.trim() }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "농장 생성에 실패했습니다.");
      }

      // 회원가입 성공 시 인증 상태 업데이트 후 메인 페이지로 이동
      await checkAuth();
      router.push("/");
    } catch (err) {
      console.error("농장 생성 오류:", err);
      setError(
        err instanceof Error ? err.message : "농장 생성에 실패했습니다."
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-light rounded-3 py-5 px-4 px-md-5 mb-5">
      <div className="row gx-5 justify-content-center">
        <div className="col-lg-8 col-xl-6">
          <div className="text-center mb-5">
            <h1 className="fw-bolder">농장 만들기</h1>
            <p className="lead fw-normal text-muted mb-0">
              나만의 농장 이름을 지어주세요
            </p>
          </div>

          {error && (
            <div className="alert alert-danger mb-4" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* 농장 이름 입력 */}
            <div className="form-floating mb-3">
              <input
                className={`form-control ${nameError ? "is-invalid" : ""}`}
                id="farmName"
                type="text"
                placeholder="농장 이름을 입력하세요"
                value={farmName}
                onChange={(e) => setFarmName(e.target.value)}
                required
                maxLength={8}
              />
              <label htmlFor="farmName">농장 이름</label>
              {nameError && <div className="invalid-feedback">{nameError}</div>}
              <div className="form-text">
                농장 이름은 8글자 이내로 입력해주세요.
              </div>
            </div>

            {/* 제출 버튼 */}
            <div className="d-grid">
              <button
                className="btn btn-secondary btn-lg"
                type="submit"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <div
                      style={{
                        display: "inline-block",
                        width: "20px",
                        height: "20px",
                        marginRight: "8px",
                        verticalAlign: "middle",
                      }}
                    >
                      <LoadingSpinner width={20} height={20} />
                    </div>
                    처리 중...
                  </>
                ) : (
                  "농장 만들기"
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

// 로딩 중 컴포넌트
function LoadingContent() {
  return (
    <div className="bg-light rounded-3 py-5 px-4 px-md-5 mb-5 text-center">
      <LoadingSpinner width={80} height={80} />
      <p className="mt-3">페이지를 준비하는 중입니다...</p>
    </div>
  );
}

// 메인 페이지 컴포넌트
export default function SignupPage() {
  return (
    <main className="flex-shrink-0">
      <section className="py-5">
        <div className="container px-5">
          <Suspense fallback={<LoadingContent />}>
            <SignupContent />
          </Suspense>
        </div>
      </section>
    </main>
  );
}
