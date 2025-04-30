"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { PostDTO } from "@/components/types/schema";

export default function WritePage() {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const { user } = useAuthStore();

  // 제목 변경 핸들러
  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(e.target.value);
  };

  // 내용 변경 핸들러
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
  };

  // 글 작성 완료 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 로그인 여부 확인
    if (!user) {
      setError("로그인이 필요합니다.");
      return;
    }

    // 유효성 검사
    if (title.trim() === "") {
      setError("제목을 입력해주세요.");
      return;
    }

    if (content.trim() === "") {
      setError("내용을 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // API 요청 보내기
      const response = await fetch("https://grow-farm.com/api/board/write", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          userId: user.userId,
          farmName: user.farmName,
          title: title,
          content: content,
        }),
      });

      if (!response.ok) {
        throw new Error(`게시글 작성에 실패했습니다 (${response.status})`);
      }

      // 응답 데이터 파싱
      const newPost: PostDTO = await response.json();

      // 성공 메시지 표시
      alert("게시글이 성공적으로 작성되었습니다.");

      // 작성된 게시글 페이지로 이동
      router.push(`/board/${newPost.postId}`);
    } catch (err) {
      console.error("게시글 작성 오류:", err);
      setError("게시글 작성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
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
                  />
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
                ></textarea>
              </section>

              {/* 버튼 영역 */}
              <div className="d-flex justify-content-end p-3">
                <Link href="/board" className="btn btn-outline-secondary me-2">
                  취소
                </Link>
                <button
                  type="submit"
                  className="btn btn-outline-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "작성 중..." : "작성 완료"}
                </button>
              </div>
            </article>
          </form>
        </div>
      </div>
    </main>
  );
}
