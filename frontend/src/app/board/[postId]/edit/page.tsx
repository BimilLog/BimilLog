"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import useAuthStore from "@/util/authStore";
import { PostDTO } from "@/components/types/schema";
import fetchClient from "@/util/fetchClient";
import { validateNoXSS, escapeHTML } from "@/util/inputValidation";
import { validatePostTitle, validatePostContent } from "@/util/boardValidation";

const API_BASE = "http://localhost:8080";

export default function EditPage() {
  const { postId } = useParams();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const { user } = useAuthStore();

  // 게시글 데이터 불러오기
  useEffect(() => {
    if (!postId || !user) return;

    const fetchPost = async () => {
      setIsLoading(true);
      try {
        const response = await fetchClient(`${API_BASE}/board/${postId}`);

        if (!response.ok) {
          throw new Error(
            `게시글을 불러오는데 실패했습니다 (${response.status})`
          );
        }

        const post: PostDTO = await response.json();

        // 자신의 게시글인지 확인
        if (post.userId !== user.userId) {
          setError("본인의 게시글만 수정할 수 있습니다.");
          return;
        }

        setTitle(post.title);
        setContent(post.content);
      } catch (err) {
        console.error("게시글 불러오기 오류:", err);
        setError(
          "게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요."
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchPost();
  }, [postId, user]);

  // 제목 변경 핸들러
  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(e.target.value);
  };

  // 내용 변경 핸들러
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
  };

  // 글 수정 완료 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 로그인 여부 확인
    if (!user) {
      setError("로그인이 필요합니다.");
      return;
    }

    // 유효성 검사 - 제목
    const [isTitleValid, titleError] = validatePostTitle(title);
    if (!isTitleValid) {
      setError(titleError);
      if (titleError) {
        alert(titleError);
      }
      return;
    }

    // 유효성 검사 - 내용
    const [isContentValid, contentError] = validatePostContent(content);
    if (!isContentValid) {
      setError(contentError);
      if (contentError) {
        alert(contentError);
      }
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // API 요청 보내기
      const response = await fetchClient(
        `${API_BASE}/board/${postId}?userId=${user.userId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            userId: user.userId,
            farmName: user.farmName,
            title: escapeHTML(title),
            content: escapeHTML(content),
          }),
        }
      );

      if (!response.ok) {
        throw new Error(`게시글 수정에 실패했습니다 (${response.status})`);
      }

      // 응답 데이터 확인 (사용하지 않음)
      await response.json();

      // 성공 메시지 표시
      alert("게시글이 성공적으로 수정되었습니다.");

      // 수정된 게시글 페이지로 이동
      router.push(`/board/${postId}`);
    } catch (err) {
      console.error("게시글 수정 오류:", err);
      setError("게시글 수정에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <main className="flex-shrink-0">
        <div className="container px-5 my-5 text-center">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">로딩중...</span>
          </div>
        </div>
      </main>
    );
  }

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
                <Link
                  href={`/board/${postId}`}
                  className="btn btn-outline-secondary me-2"
                >
                  취소
                </Link>
                <button
                  type="submit"
                  className="btn btn-outline-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "수정 중..." : "수정 완료"}
                </button>
              </div>
            </article>
          </form>
        </div>
      </div>
    </main>
  );
}
