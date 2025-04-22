"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState, useEffect } from "react";
import { FarmNameRequestDTO } from "@/components/types/schema";

export default function SignupPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [tokenId, setTokenId] = useState<number | null>(null);
  const [farmName, setFarmName] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);

  useEffect(() => {
    const tokenIdParam = searchParams.get("tokenId");
    if (!tokenIdParam) {
      setError("토큰 정보가 없습니다. 다시 로그인해주세요.");
      router.replace("/");
      return;
    }
    setTokenId(parseInt(tokenIdParam, 10));
  }, [searchParams]);

  const validateFarmName = (name: string): boolean => {
    if (name.trim().length < 2) {
      setNameError("농장 이름은 2글자 이상이어야 합니다.");
      return false;
    }
    if (name.trim().length > 8) {
      setNameError("농장 이름은 8글자 이하여야 합니다.");
      return false;
    }
    setNameError(null);
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateFarmName(farmName)) {
      return;
    }

    if (!tokenId) {
      setError("토큰 정보가 없습니다. 다시 로그인해주세요.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const farmNameRequestDTO: FarmNameRequestDTO = {
        tokenId: tokenId,
        farmName: farmName,
      };

      const response = await fetch("http://localhost:8080/auth/signUp", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(farmNameRequestDTO),
        credentials: "include",
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "농장 생성에 실패했습니다.");
      }

      // 회원가입 성공 시 메인 페이지로 이동
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
    <main className="flex-shrink-0">
      <section className="py-5">
        <div className="container px-5">
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
                      className={`form-control ${
                        nameError ? "is-invalid" : ""
                      }`}
                      id="farmName"
                      type="text"
                      placeholder="농장 이름을 입력하세요"
                      value={farmName}
                      onChange={(e) => setFarmName(e.target.value)}
                      required
                    />
                    <label htmlFor="farmName">농장 이름</label>
                    {nameError && (
                      <div className="invalid-feedback">{nameError}</div>
                    )}
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
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                          ></span>
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
        </div>
      </section>
    </main>
  );
}
