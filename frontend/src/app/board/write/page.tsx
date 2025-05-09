"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import LoadingSpinner from "@/components/LoadingSpinner";
import fetchClient from "@/util/fetchClient";
import { validateNoXSS, escapeHTML } from "@/util/inputValidation";
import { validatePostTitle, validatePostContent } from "@/util/boardValidation";

const API_BASE = "https://grow-farm.com/api";

export default function WritePage() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const { user } = useAuthStore();

  // 제목 변경 핸들러
  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (validateNoXSS(e.target.value)) {
      setTitle(e.target.value);
    }
  };

  // 내용 변경 핸들러
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (validateNoXSS(e.target.value)) {
      setContent(e.target.value);
    }
  };

  // 게시글 작성 제출 처리
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 유효성 검사 - 제목
    const [isTitleValid, titleError] = validatePostTitle(title);
    if (!isTitleValid) {
      setError(titleError);
      if (titleError && titleError.includes("30자")) {
        alert(titleError);
      }
      return;
    }

    // 유효성 검사 - 내용
    const [isContentValid, contentError] = validatePostContent(content);
    if (!isContentValid) {
      setError(contentError);
      if (contentError && contentError.includes("1000자")) {
        alert(contentError);
      }
      return;
    }

    // 로그인 확인
    if (!user) {
      if (
        confirm(
          "로그인이 필요한 서비스입니다. 로그인 페이지로 이동하시겠습니까?"
        )
      ) {
        router.push("/");
      }
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await fetchClient(`${API_BASE}/board/write`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          title: escapeHTML(title),
          content: escapeHTML(content),
        }),
      });

      if (response.ok) {
        const data = await response.json();
        const postId = data.postId;
        router.push(`/board/${postId}`);
      } else {
        const errorText = await response.text();
        throw new Error(errorText || "게시글 작성에 실패했습니다.");
      }
    } catch (error) {
      console.error("게시글 작성 오류:", error);
      setError(
        error instanceof Error
          ? error.message
          : "게시글 작성 중 오류가 발생했습니다."
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="flex-shrink-0">
      <div className="container px-5 my-5">
        <div className="col-lg-0">
          {error && (
            <div className="alert alert-danger mb-3" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <article className="card bg-white">
              <header className="mb-4 card bg-light">
                <div className="p-3">
                  <input
                    type="text"
                    className="form-control form-control-lg"
                    placeholder="제목을 입력하세요"
                    value={title}
                    onChange={handleTitleChange}
                    required
                    maxLength={30}
                  />
                  <div className="form-text">
                    제목은 30자 이내로 입력해주세요.
                  </div>
                </div>
              </header>
              {/* 내용 텍스트 에어리어 */}
              <section className="mb-3 px-4">
                <textarea
                  className="form-control"
                  placeholder="내용을 입력하세요"
                  rows={15}
                  value={content}
                  onChange={handleContentChange}
                  required
                  maxLength={1000}
                ></textarea>
                <div className="form-text text-end">
                  {content.length}/1000자
                </div>
              </section>

              {/* 버튼 영역 */}
              <div className="d-flex justify-content-end p-3">
                <Link href="/board" className="btn btn-outline-secondary me-2">
                  취소
                </Link>
                <button
                  type="submit"
                  className="btn btn-outline-primary"
                  disabled={isLoading}
                >
                  {isLoading ? <LoadingSpinner /> : "작성 완료"}
                </button>
              </div>
            </article>
          </form>
        </div>
      </div>
    </main>
  );
}
