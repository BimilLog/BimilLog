"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import {
  PostDTO,
  CommentDTO,
  ReportType,
  ReportDTO,
} from "@/components/types/schema";
import { formatDateTime } from "@/util/date";
import useAuthStore from "@/util/authStore";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import Script from "next/script";
import fetchClient from "@/util/fetchClient";
import SafeHTML from "@/components/SafeHTML";
import { sanitizeHtml } from "@/util/sanitize";
import { validateNoXSS, escapeHTML } from "@/util/inputValidation";

// 로딩 스피너 컴포넌트
const LoadingSpinner = () => (
  <div className="text-center py-4">
    <div className="spinner-border text-primary" role="status">
      <span className="visually-hidden">로딩중...</span>
    </div>
  </div>
);

// API 경로
const API_BASE = "https://grow-farm.com/api";



export default function PostPage() {
  const { postId } = useParams();
  const router = useRouter();
  const [post, setPost] = useState<PostDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [commentContent, setCommentContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState("");
  const [showShareOptions, setShowShareOptions] = useState(false);
  const shareDropdownRef = useRef<HTMLDivElement>(null);
  const { user, isInitialized, checkAuth } = useAuthStore();
  const javaScriptKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;

  // Kakao SDK 초기화
  const initKakao = useCallback(() => {
    if (!javaScriptKey) {
      console.error("Kakao JavaScript Key is not defined.");
      return;
    }

    if (window.Kakao && !window.Kakao.isInitialized()) {
      window.Kakao.init(javaScriptKey);
      console.log("Kakao SDK initialized");
    }
  }, [javaScriptKey]);

  // 링크 복사 함수
  const copyLinkToClipboard = () => {
    const currentUrl = window.location.href;
    navigator.clipboard
      .writeText(currentUrl)
      .then(() => {
        alert("링크가 클립보드에 복사되었습니다.");
        setShowShareOptions(false);
      })
      .catch((err) => {
        console.error("링크 복사 실패:", err);
        alert("링크 복사에 실패했습니다. 다시 시도해주세요.");
      });
  };

  // 카카오톡 공유하기 함수
  const shareToKakao = () => {
    if (!post) return;

    if (!window.Kakao || !window.Kakao.isInitialized()) {
      console.error("Kakao SDK is not initialized");
      alert(
        "카카오톡 공유 기능을 사용할 수 없습니다. 잠시 후 다시 시도해주세요."
      );
      return;
    }

    // 현재 URL
    const currentUrl = window.location.href;

    // 피드 템플릿 A형으로 메시지 보내기
    window.Kakao.Share.sendDefault({
      objectType: "feed",
      content: {
        title: post.title, // B영역: 글 제목
        imageUrl:
          "https://postfiles.pstatic.net/MjAyNTA0MThfNzcg/MDAxNzQ0OTc4MDY3NjU2.b2ZRY2ZhuqdeFe8R70IoJZ0gGm4XTFZgKrZqNqQYinkg.vorO6lPc33dEIhZqQ7PbrwjOH7qn9-RfkOJAEVA2I2cg.JPEG/farmImage.jpeg?type=w773", // A영역: 이미지 (명시적 URL 적용)
        link: {
          mobileWebUrl: currentUrl,
          webUrl: currentUrl,
        },
      },
      buttons: [
        {
          title: "웹으로 보기",
          link: {
            mobileWebUrl: currentUrl,
            webUrl: currentUrl,
          },
        },
      ],
    });

    setShowShareOptions(false);
  };

  // 공유 옵션 토글
  const toggleShareOptions = () => {
    setShowShareOptions((prev) => !prev);
  };

  // 외부 클릭 시 공유 옵션 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        shareDropdownRef.current &&
        !shareDropdownRef.current.contains(event.target as Node)
      ) {
        setShowShareOptions(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // Kakao SDK 초기화
  useEffect(() => {
    initKakao();
  }, [initKakao]);

  // 신고 관련 상태
  const [reportingTarget, setReportingTarget] = useState<{
    id: number;
    type: ReportType;
  } | null>(null);
  const [reportContent, setReportContent] = useState("");

  // 사용자 인증 상태 확인
  useEffect(() => {
    if (!isInitialized) checkAuth();
  }, [isInitialized, checkAuth]);

  // 게시글 데이터 불러오기 - 인증 상태와 무관하게 실행
  useEffect(() => {
    if (!postId) return;

    const fetchPost = async () => {
      setLoading(true);
      try {
        const userIdParam = user ? `?userId=${user.userId}` : "";
        const response = await fetchClient(
          `${API_BASE}/board/${postId}${userIdParam}`
        );

        if (!response.ok) {
          throw new Error(
            `게시글을 불러오는데 실패했습니다 (${response.status})`
          );
        }

        const data = await response.json();
        setPost(data);
      } catch (err) {
        console.error("게시글 불러오기 오류:", err);
        setError(
          "게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요."
        );
      } finally {
        setLoading(false);
      }
    };

    fetchPost();
  }, [postId, user]);

  // 댓글 제출 핸들러
  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentContent.trim() || !user || isSubmitting) return;

    if (commentContent.length > 255) {
      alert("댓글은 255자 이내로 작성해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      // CommentDTO 형식에 맞게 데이터 구성
      const commentDTO = {
        postId: Number(postId),
        userId: user.userId,
        userName: user.userName,
        content: commentContent,
      };

      const response = await fetchClient(
        `${API_BASE}/board/${postId}/comment`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(commentDTO),
        }
      );

      if (!response.ok) throw new Error("댓글 작성에 실패했습니다.");

      // 댓글 작성 후 게시글 다시 불러오기
      const updatedPost = await fetchClient(`${API_BASE}/board/${postId}`).then(
        (res) => res.json()
      );

      setPost(updatedPost);
      setCommentContent("");
    } catch (err) {
      console.error("댓글 작성 오류:", err);
      alert("댓글 작성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // 댓글 수정 시작 핸들러
  const handleEditComment = (comment: CommentDTO) => {
    setEditingCommentId(comment.id);
    setEditingCommentContent(comment.content);
  };

  // 댓글 수정 취소 핸들러
  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditingCommentContent("");
  };

  // 댓글 수정 저장 핸들러
  const handleSaveComment = async (commentId: number) => {
    if (!user || isSubmitting || !editingCommentContent.trim()) return;

    if (!validateNoXSS(editingCommentContent)) {
      alert("특수문자(<, >, &, \", ')는 사용이 불가능합니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await fetchClient(
        `${API_BASE}/board/comment/${commentId}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            content: escapeHTML(editingCommentContent),
            userId: user.userId,
          }),
        }
      );

      if (!response.ok) throw new Error("댓글 수정에 실패했습니다.");

      // 댓글 수정 후 게시글 다시 불러오기
      const updatedPost = await fetchClient(`${API_BASE}/board/${postId}`).then(
        (res) => res.json()
      );

      setPost(updatedPost);
      setEditingCommentId(null);
      setEditingCommentContent("");
    } catch (err) {
      console.error("댓글 수정 오류:", err);
      alert("댓글 수정에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // 댓글 삭제 핸들러
  const handleDeleteComment = async (commentId: number) => {
    if (!user || isSubmitting) return;

    // 삭제 확인
    if (!confirm("정말로 이 댓글을 삭제하시겠습니까?")) return;

    setIsSubmitting(true);
    try {
      const response = await fetchClient(
        `${API_BASE}/board/${postId}/${commentId}/delete`,
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        throw new Error("댓글 삭제에 실패했습니다.");
      }

      // 댓글 삭제 후 게시글 다시 불러오기
      const updatedPost = await fetchClient(`${API_BASE}/board/${postId}`).then(
        (res) => res.json()
      );

      setPost(updatedPost);
    } catch (err) {
      console.error("댓글 삭제 오류:", err);
      alert("댓글 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // 댓글 추천 핸들러
  const handleLikeComment = async (comment: CommentDTO) => {
    if (!user || isSubmitting) return;

    setIsSubmitting(true);

    // 현재 댓글 찾기
    const commentIndex = post!.comments.findIndex((c) => c.id === comment.id);
    if (commentIndex === -1) {
      setIsSubmitting(false);
      return;
    }

    // 낙관적 UI 업데이트
    const isCurrentlyLiked = comment.userLike;
    const likesChange = isCurrentlyLiked ? -1 : 1;

    const updatedComments = [...post!.comments];
    updatedComments[commentIndex] = {
      ...updatedComments[commentIndex],
      userLike: !isCurrentlyLiked,
      likes: updatedComments[commentIndex].likes + likesChange,
    };

    setPost({
      ...post!,
      comments: updatedComments,
    });

    try {
      const response = await fetchClient(
        `${API_BASE}/board/${postId}/${comment.id}/like`,
        { method: "POST" }
      );

      if (!response.ok) {
        // 실패 시 원래 상태로 복원
        const restoredComments = [...post!.comments];
        restoredComments[commentIndex] = {
          ...restoredComments[commentIndex],
          userLike: isCurrentlyLiked,
          likes: restoredComments[commentIndex].likes - likesChange,
        };

        setPost({
          ...post!,
          comments: restoredComments,
        });

        throw new Error("댓글 추천에 실패했습니다.");
      }
    } catch (err) {
      console.error("댓글 추천 오류:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 게시글 추천 핸들러
  const handleLike = async () => {
    if (!user || isSubmitting || !post) return;

    setIsSubmitting(true);
    // 낙관적 UI 업데이트
    const isCurrentlyLiked = post.userLike;
    const likesChange = isCurrentlyLiked ? -1 : 1;

    setPost({
      ...post,
      userLike: !isCurrentlyLiked,
      likes: post.likes + likesChange,
    });

    try {
      const response = await fetchClient(
        `${API_BASE}/board/${postId}/like?userId=${user.userId}`,
        { method: "POST" }
      );

      if (!response.ok) {
        // 실패 시 원래 상태로 복원
        setPost({
          ...post,
          userLike: isCurrentlyLiked,
          likes: post.likes,
        });
        throw new Error("추천에 실패했습니다.");
      }
    } catch (err) {
      console.error("추천 오류:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 게시글 삭제 핸들러
  const handleDelete = async () => {
    if (!user || !post || isSubmitting) return;

    // 삭제 확인
    if (!confirm("정말로 이 게시글을 삭제하시겠습니까?")) return;

    setIsSubmitting(true);

    try {
      const response = await fetchClient(`${API_BASE}/board/${postId}/delete`, {
        method: "POST",
      });

      if (!response.ok) {
        throw new Error("게시글 삭제에 실패했습니다.");
      }

      alert("게시글이 성공적으로 삭제되었습니다.");
      router.push("/board");
    } catch (err) {
      console.error("게시글 삭제 오류:", err);
      alert("게시글 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // 신고 시작 핸들러
  const handleStartReport = (id: number, type: ReportType) => {
    setReportingTarget({ id, type });
    setReportContent("");
  };

  // 신고 취소 핸들러
  const handleCancelReport = () => {
    setReportingTarget(null);
    setReportContent("");
  };

  // 신고 제출 핸들러
  const handleSubmitReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !reportingTarget || !reportContent.trim() || isSubmitting)
      return;

    if (reportContent.length > 500) {
      alert("신고 내용은 500자 이내로 작성해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      let response;

      // 게시글 신고와 댓글 신고에 따라 다른 엔드포인트 사용
      if (reportingTarget.type === ReportType.POST) {
        // 게시글 신고
        const reportDTO: ReportDTO = {
          reportId: 0,
          reportType: ReportType.POST,
          userId: user.userId,
          targetId: Number(postId),
          content: reportContent,
        };

        response = await fetchClient(`${API_BASE}/board/${postId}/report`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(reportDTO),
        });
      } else if (reportingTarget.type === ReportType.COMMENT) {
        // 댓글 신고
        const reportDTO: ReportDTO = {
          reportId: 0,
          reportType: ReportType.COMMENT,
          userId: user.userId,
          targetId: reportingTarget.id,
          content: reportContent,
        };

        response = await fetchClient(
          `${API_BASE}/board/${postId}/comment/report`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(reportDTO),
          }
        );
      } else {
        // 기타 유형의 신고
        response = await fetchClient(`${API_BASE}/report`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            reportType: reportingTarget.type,
            userId: user.userId,
            targetId: reportingTarget.id,
            content: reportContent,
          }),
        });
      }

      if (!response.ok) throw new Error("신고 제출에 실패했습니다.");

      alert("신고가 성공적으로 접수되었습니다.");
      setReportingTarget(null);
      setReportContent("");
    } catch (err) {
      console.error("신고 오류:", err);
      alert("신고 접수에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <main className="container px-5 my-5">
        <LoadingSpinner />
      </main>
    );
  }

  if (error || !post) {
    return (
      <main className="container px-5 my-5">
        <div className="alert alert-danger" role="alert">
          {error || "게시글을 불러오는데 실패했습니다."}
        </div>
        <div className="text-center mt-4">
          <Link href="/board" className="btn btn-outline-primary">
            게시판으로 돌아가기
          </Link>
        </div>
      </main>
    );
  }

  const isAuthor = user && user.userId === post.userId;

  return (
    <main className="container px-5 my-5">
      {/* Kakao SDK 스크립트 */}
      <Script
        src="https://t1.kakaocdn.net/kakao_js_sdk/2.5.0/kakao.min.js"
        integrity="sha384-kYPsUbBPlktXsY6/oNHSUDZoTX6+YI51f63jCPEIPFP09ttByAdxd2mEjKuhdqn4"
        crossOrigin="anonymous"
        onLoad={initKakao}
      />

      <article className="card bg-white">
        <header className="mb-4 card bg-light">
          <h1 className="fw-bolder pt-4 pb-2 text-center">
            <SafeHTML html={sanitizeHtml(post.title)} />
          </h1>
          <div className="fw-bold text-xl-end mx-3">
            작성농장 : {post.userName}
          </div>
          <div className="text-muted fw-bold mb-2 d-flex justify-content-end gap-4 mx-3">
            <div>조회: {post.views}</div>
            <div>작성일: {formatDateTime(post.createdAt)}</div>
          </div>
        </header>

        <section className="mb-3 px-4">
          <div
            className="fs-5 mb-4"
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(post.content) }}
          />
        </section>

        <div className="d-flex justify-content-between align-items-center px-4 mb-4">
          <div className="d-flex gap-2">
            {user && user.userId !== post.userId ? (
              <button
                className={`btn ${
                  post.userLike ? "btn-danger" : "btn-outline-primary"
                }`}
                onClick={handleLike}
                disabled={isSubmitting}
              >
                <i className="bi bi-hand-thumbs-up"></i> 추천 ({post.likes})
              </button>
            ) : (
              <button className="btn btn-outline-primary" disabled={true}>
                <i className="bi bi-hand-thumbs-up"></i> 추천 ({post.likes})
              </button>
            )}
            <div className="position-relative" ref={shareDropdownRef}>
              <button
                className="btn btn-outline-secondary"
                onClick={toggleShareOptions}
              >
                <i className="bi bi-share"></i> 공유하기
              </button>

              {showShareOptions && (
                <div
                  className="position-absolute start-0 mt-1 bg-white border rounded shadow-sm"
                  style={{ zIndex: 1000, minWidth: "200px" }}
                >
                  <ul className="list-group list-group-flush">
                    <li
                      className="list-group-item list-group-item-action"
                      onClick={copyLinkToClipboard}
                      style={{ cursor: "pointer" }}
                    >
                      <i className="bi bi-clipboard me-2"></i> 링크 복사
                    </li>
                    <li
                      className="list-group-item list-group-item-action"
                      onClick={shareToKakao}
                      style={{ cursor: "pointer" }}
                    >
                      <i className="bi bi-chat-fill me-2 text-warning"></i>{" "}
                      카카오톡 공유
                    </li>
                  </ul>
                </div>
              )}
            </div>
            {user && user.userId !== post.userId && (
              <button
                className="btn btn-outline-warning"
                onClick={() =>
                  handleStartReport(Number(postId), ReportType.POST)
                }
                disabled={isSubmitting}
              >
                <i className="bi bi-exclamation-triangle"></i> 신고
              </button>
            )}
          </div>

          {isAuthor && (
            <div className="d-flex gap-2">
              <button
                className="btn btn-outline-secondary"
                onClick={() => router.push(`/board/${postId}/edit`)}
              >
                <i className="bi bi-pencil"></i> 수정
              </button>
              <button
                className="btn btn-outline-danger"
                onClick={handleDelete}
                disabled={isSubmitting}
              >
                <i className="bi bi-trash"></i> 삭제
              </button>
            </div>
          )}
        </div>

        {/* 게시글 신고 폼 */}
        {reportingTarget && reportingTarget.type === ReportType.POST && (
          <div className="px-4 pb-4">
            <div className="card border-warning">
              <div className="card-header bg-warning bg-opacity-10">
                <strong>게시글 신고</strong>
              </div>
              <div className="card-body">
                <form onSubmit={handleSubmitReport}>
                  <div className="mb-3">
                    <label htmlFor="reportContent" className="form-label">
                      신고 사유
                    </label>
                    <textarea
                      id="reportContent"
                      className="form-control"
                      rows={3}
                      placeholder="신고 사유를 입력하세요..."
                      value={reportContent}
                      onChange={(e) => setReportContent(e.target.value)}
                      required
                      maxLength={500}
                    />
                    <div className="form-text text-end">
                      {reportContent.length}/500자
                    </div>
                  </div>
                  <div className="d-flex justify-content-end gap-2">
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={handleCancelReport}
                    >
                      취소
                    </button>
                    <button
                      type="submit"
                      className="btn btn-warning"
                      disabled={isSubmitting || !reportContent.trim()}
                    >
                      신고하기
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
      </article>

      <section className="card bg-light mt-4">
        <div className="card-header">
          <h5 className="mb-0">댓글 ({post.comments?.length || 0})</h5>
        </div>
        <div className="card-body">
          {user ? (
            <form className="mb-4" onSubmit={handleCommentSubmit}>
              <textarea
                className="form-control"
                rows={3}
                placeholder="댓글을 입력하세요..."
                value={commentContent}
                onChange={(e) => {
                  if (validateNoXSS(e.target.value)) {
                    setCommentContent(e.target.value);
                  }
                }}
                maxLength={255}
              />
              <div className="d-flex justify-content-between mt-2">
                <div className="form-text">{commentContent.length}/255자</div>
                <button
                  type="submit"
                  className="btn btn-outline-secondary"
                  disabled={isSubmitting || !commentContent.trim()}
                >
                  댓글 작성
                </button>
              </div>
            </form>
          ) : (
            <div className="alert alert-info mb-4">
              댓글을 작성하려면{" "}
              <Link href="/login" className="alert-link">
                로그인
              </Link>
              이 필요합니다.
            </div>
          )}

          {post.comments?.length > 0 ? (
            post.comments.map((comment) => (
              <div className="d-flex mb-4" key={comment.id}>
                <div className="ms-3 w-100">
                  <div className="d-flex justify-content-between">
                    <div className="fw-bold">{comment.userName}</div>
                    <small className="text-muted">
                      {formatDateTime(comment.createdAt)}
                    </small>
                  </div>

                  {editingCommentId === comment.id ? (
                    <div className="mt-2 mb-2">
                      <textarea
                        className="form-control"
                        value={editingCommentContent}
                        onChange={(e) =>
                          setEditingCommentContent(e.target.value)
                        }
                        rows={3}
                        maxLength={255}
                      />
                      <div className="d-flex justify-content-between mt-2">
                        <div className="form-text">
                          {editingCommentContent.length}/255자
                        </div>
                        <div className="d-flex gap-2">
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            onClick={handleCancelEdit}
                            disabled={isSubmitting}
                          >
                            취소
                          </button>
                          <button
                            className="btn btn-sm btn-outline-primary"
                            onClick={() => handleSaveComment(comment.id)}
                            disabled={
                              isSubmitting || !editingCommentContent.trim()
                            }
                          >
                            저장
                          </button>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="d-flex justify-content-between align-items-center mt-2">
                        <p className="mb-0 me-2" style={{ flex: "1" }}>
                          <SafeHTML html={sanitizeHtml(comment.content)} />
                        </p>
                        <div className="d-flex align-items-center gap-2 flex-shrink-0">
                          {user && user.userId !== comment.userId ? (
                            <button
                              className={`btn btn-sm ${
                                comment.userLike
                                  ? "btn-danger"
                                  : "btn-outline-primary"
                              }`}
                              onClick={() => handleLikeComment(comment)}
                              disabled={isSubmitting}
                            >
                              <i className="bi bi-hand-thumbs-up"></i> 추천 (
                              {comment.likes})
                            </button>
                          ) : (
                            <button
                              className="btn btn-sm btn-outline-primary"
                              disabled={true}
                            >
                              <i className="bi bi-hand-thumbs-up"></i> 추천 (
                              {comment.likes})
                            </button>
                          )}

                          {user && user.userId !== comment.userId && (
                            <button
                              className="btn btn-sm btn-outline-warning"
                              onClick={() =>
                                handleStartReport(
                                  comment.id,
                                  ReportType.COMMENT
                                )
                              }
                              disabled={isSubmitting}
                            >
                              <i className="bi bi-exclamation-triangle"></i>{" "}
                              신고
                            </button>
                          )}

                          {user && user.userId === comment.userId && (
                            <>
                              <button
                                className="btn btn-sm btn-outline-secondary"
                                onClick={() => handleEditComment(comment)}
                              >
                                <i className="bi bi-pencil"></i> 수정
                              </button>
                              <button
                                className="btn btn-sm btn-outline-danger"
                                onClick={() => handleDeleteComment(comment.id)}
                                disabled={isSubmitting}
                              >
                                <i className="bi bi-trash"></i> 삭제
                              </button>
                            </>
                          )}
                        </div>
                      </div>
                    </>
                  )}

                  {/* 댓글 신고 폼 */}
                  {reportingTarget &&
                    reportingTarget.type === ReportType.COMMENT &&
                    reportingTarget.id === comment.id && (
                      <div className="mt-3">
                        <div className="card border-warning">
                          <div className="card-header bg-warning bg-opacity-10">
                            <strong>댓글 신고</strong>
                          </div>
                          <div className="card-body">
                            <form onSubmit={handleSubmitReport}>
                              <div className="mb-3">
                                <label
                                  htmlFor={`reportContent-${comment.id}`}
                                  className="form-label"
                                >
                                  신고 사유
                                </label>
                                <textarea
                                  id={`reportContent-${comment.id}`}
                                  className="form-control"
                                  rows={3}
                                  placeholder="신고 사유를 입력하세요..."
                                  value={reportContent}
                                  onChange={(e) =>
                                    setReportContent(e.target.value)
                                  }
                                  required
                                  maxLength={500}
                                />
                                <div className="form-text text-end">
                                  {reportContent.length}/500자
                                </div>
                              </div>
                              <div className="d-flex justify-content-end gap-2">
                                <button
                                  type="button"
                                  className="btn btn-sm btn-outline-secondary"
                                  onClick={handleCancelReport}
                                >
                                  취소
                                </button>
                                <button
                                  type="submit"
                                  className="btn btn-sm btn-warning"
                                  disabled={
                                    isSubmitting || !reportContent.trim()
                                  }
                                >
                                  신고하기
                                </button>
                              </div>
                            </form>
                          </div>
                        </div>
                      </div>
                    )}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-4 text-muted">
              첫 댓글을 작성해보세요.
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
