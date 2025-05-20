"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import useAuthStore from "@/util/authStore";
import fetchClient from "@/util/fetchClient";
import { FarmNameReqDTO } from "@/components/types/schema";
import { validateNoXSS, escapeHTML } from "@/util/inputValidation";

const API_BASE = "http://localhost:8080";

export default function MyPage() {
  const { user, setUser, logout } = useAuthStore();
  const [isEditingFarmName, setIsEditingFarmName] = useState(false);
  const [newFarmName, setNewFarmName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);

  const handleFarmNameChange = async (e: FormEvent) => {
    e.preventDefault();

    const trimmedFarmName = newFarmName.trim();
    if (!trimmedFarmName) {
      alert("새 농장 이름을 입력해주세요.");
      return;
    }

    if (trimmedFarmName.length > 8) {
      alert("농장 이름은 8글자 이하여야 합니다.");
      return;
    }

    if (!validateNoXSS(trimmedFarmName)) {
      alert("특수문자(<, >, &, \", ')는 사용이 불가능합니다.");
      return;
    }

    if (!user) return;

    if (!confirm("농장이름을 바꾸면 다시 로그인해야합니다.")) {
      return;
    }

    setIsLoading(true);

    try {
      const requestBody: Partial<FarmNameReqDTO> = {
        farmName: escapeHTML(trimmedFarmName),
      };
      const response = await fetchClient(`${API_BASE}/user/mypage/updatefarm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody),
      });

      await response.text();

      if (response.ok) {
        setUser({ ...user, farmName: trimmedFarmName });
        setIsEditingFarmName(false);
        setNewFarmName("");
        await logout();
      } else {
        alert(
          `농장 이름 변경에 실패했습니다: ${response.status} ${response.statusText}`
        );
      }
    } catch (error: unknown) {
      console.error("농장 이름 변경 중 오류 발생:", error);
      alert(
        `농장 이름 변경 중 오류가 발생했습니다: ${
          error instanceof Error ? error.message : "알 수 없는 오류"
        }`
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleWithdraw = async () => {
    if (
      !confirm(
        "정말 회원탈퇴를 하시겠습니까? 회원탈퇴를 하시면 관련된 데이터는 전부 지워집니다."
      )
    ) {
      return;
    }

    setIsWithdrawing(true);
    try {
      const response = await fetchClient(`${API_BASE}/auth/withdraw`, {
        method: "POST",
      });

      if (response.ok) {
        alert("회원 탈퇴가 완료되었습니다.");
        window.location.href = "/";
      } else {
        const errorText = await response.text();
        alert(
          `회원 탈퇴에 실패했습니다: ${response.status} ${response.statusText}`
        );
        console.error("회원 탈퇴 실패:", errorText);
      }
    } catch (error: unknown) {
      console.error("회원 탈퇴 중 오류 발생:", error);
      alert(
        `회원 탈퇴 중 오류가 발생했습니다: ${
          error instanceof Error ? error.message : "알 수 없는 오류"
        }`
      );
    } finally {
      setIsWithdrawing(false);
    }
  };

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 py-5">
        <div className="row">
          {/* 프로필 섹션 */}
          <div className="col-md-4">
            <div className="card mb-4">
              <div className="card-body text-center">
                {/* 프로필 이미지 */}
                <img
                  src={
                    user?.thumbnailImage ||
                    "https://dummyimage.com/150x150/6c757d/dee2e6.jpg"
                  }
                  alt="프로필 이미지"
                  className="img-fluid rounded-circle mb-4 px-4"
                />

                {/* 사용자 정보 */}
                <h5 className="fw-bold mb-1">
                  {user?.farmName || "농장 이름 없음"}
                </h5>
                <p className="text-muted mb-1">
                  카카오ID: {user?.kakaoId || "-"}
                </p>
                <p className="text-muted mb-3">
                  카카오 닉네임: {user?.kakaoNickname || "-"}
                </p>

                {/* 프로필 관리 버튼 및 폼 */}
                <div className="d-grid gap-2 mb-2">
                  {!isEditingFarmName ? (
                    <button
                      className="btn btn-secondary"
                      onClick={() => setIsEditingFarmName(true)}
                      disabled={isLoading || isWithdrawing}
                    >
                      <i className="bi bi-pencil-square me-1"></i>농장 이름 변경
                    </button>
                  ) : (
                    <form
                      onSubmit={handleFarmNameChange}
                      className="vstack gap-2"
                    >
                      <input
                        type="text"
                        className="form-control"
                        placeholder="새 농장 이름 입력"
                        value={newFarmName}
                        onChange={(e) => {
                          if (!validateNoXSS(e.target.value)) {
                            alert(
                              "특수문자(<, >, &, \", ')는 사용이 불가능합니다."
                            );
                            return;
                          }
                          setNewFarmName(e.target.value);
                        }}
                        required
                        disabled={isLoading || isWithdrawing}
                        maxLength={8}
                      />
                      <div className="form-text">
                        농장 이름은 8글자 이내로 입력해주세요.
                      </div>
                      <div className="hstack gap-2">
                        <button
                          type="submit"
                          className="btn btn-primary w-100"
                          disabled={
                            isLoading || !newFarmName.trim() || isWithdrawing
                          }
                        >
                          {isLoading ? "변경 중..." : "변경"}
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-secondary w-100"
                          onClick={() => {
                            setIsEditingFarmName(false);
                            setNewFarmName("");
                          }}
                          disabled={isLoading || isWithdrawing}
                        >
                          취소
                        </button>
                      </div>
                    </form>
                  )}
                  <button
                    className="btn btn-secondary text-danger"
                    disabled={isLoading || isWithdrawing}
                    onClick={handleWithdraw}
                  >
                    <i className="bi bi-person-x me-1"></i>
                    {isWithdrawing ? "처리 중..." : "회원 탈퇴"}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 내 활동 섹션 */}
          <div className="col-md-8">
            <div className="card">
              <div className="card-header">
                <h5 className="mb-0">내 활동</h5>
              </div>
              <div className="card-body">
                <div className="row mb-0">
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <Link
                        href="/mypage/mypost"
                        className="btn btn-primary py-3"
                      >
                        내가 쓴 글 보기
                      </Link>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <Link
                        href="/mypage/mycomment"
                        className="btn btn-primary py-3"
                      >
                        내가 쓴 댓글 보기
                      </Link>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <Link
                        href="/mypage/likepost"
                        className="btn btn-primary py-3"
                      >
                        추천한 글 보기
                      </Link>
                    </div>
                  </div>
                  <div className="col-md-6 mb-3">
                    <div className="d-grid">
                      <Link
                        href="/mypage/likecomment"
                        className="btn btn-primary py-3"
                      >
                        추천한 댓글 보기
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
