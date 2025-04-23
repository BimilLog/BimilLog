"use client";

import { useEffect, useState } from "react";
import { PostDTO, CommentDTO, ReportType } from "@/components/types/schema";
import { formatDateTime } from "@/util/date";
import useAuthStore from "@/util/authStore";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";

// 로딩 스피너 컴포넌트
const LoadingSpinner = () => (
  <div className="text-center py-4">
    <div className="spinner-border text-primary" role="status">
      <span className="visually-hidden">로딩중...</span>
    </div>
  </div>
);

// 댓글 컴포넌트
const Comment = ({
  comment,
  isLoggedIn,
}: {
  comment: CommentDTO;
  isLoggedIn: boolean;
}) => (
  <div className="d-flex mb-4">
    <div className="ms-3 w-100">
      <div className="d-flex justify-content-between">
        <div className="fw-bold">{comment.farmName}</div>
        <small className="text-muted">
          {formatDateTime(comment.createdAt)}
        </small>
      </div>
      <p>{comment.content}</p>
      {isLoggedIn && (
        <div className="d-flex justify-content-between align-items-center">
          <button className="btn btn-sm btn-outline-primary">
            <i className="bi bi-hand-thumbs-up"></i> 추천 ({comment.likes})
          </button>
        </div>
      )}
    </div>
  </div>
);

// API 경로
const API_BASE = "http://localhost:8080";

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
  const { user, isLoading, isInitialized, checkAuth } = useAuthStore();

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
        const response = await fetch(
          `${API_BASE}/board/${postId}${userIdParam}`,
          { credentials: "include" }
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

    setIsSubmitting(true);
    try {
      // CommentDTO 형식에 맞게 데이터 구성
      const commentDTO = {
        postId: Number(postId),
        userId: user.userId,
        farmName: user.farmName,
        content: commentContent,
      };

      const response = await fetch(`${API_BASE}/board/${postId}/comment`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(commentDTO),
      });

      if (!response.ok) throw new Error("댓글 작성에 실패했습니다.");

      // 댓글 작성 후 게시글 다시 불러오기
      const updatedPost = await fetch(`${API_BASE}/board/${postId}`, {
        credentials: "include",
      }).then((res) => res.json());

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

    setIsSubmitting(true);
    try {
      // CommentDTO 형식에 맞게 데이터 구성
      const commentDTO = {
        id: commentId,
        postId: Number(postId),
        userId: user.userId,
        farmName: user.farmName,
        content: editingCommentContent,
      };

      const response = await fetch(`${API_BASE}/board/${postId}/${commentId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(commentDTO),
      });

      if (!response.ok) throw new Error("댓글 수정에 실패했습니다.");

      // 댓글 수정 후 게시글 다시 불러오기
      const updatedPost = await fetch(`${API_BASE}/board/${postId}`, {
        credentials: "include",
      }).then((res) => res.json());

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
      const response = await fetch(
        `${API_BASE}/board/${postId}/${commentId}/delete`,
        {
          method: "POST",
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("댓글 삭제에 실패했습니다.");
      }

      // 댓글 삭제 후 게시글 다시 불러오기
      const updatedPost = await fetch(`${API_BASE}/board/${postId}`, {
        credentials: "include",
      }).then((res) => res.json());

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
      const response = await fetch(
        `${API_BASE}/board/${postId}/${comment.id}/like`,
        { method: "POST", credentials: "include" }
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
      const response = await fetch(
        `${API_BASE}/board/${postId}/like?userId=${user.userId}`,
        { method: "POST", credentials: "include" }
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
      const response = await fetch(`${API_BASE}/board/${postId}/delete`, {
        method: "POST",
        credentials: "include",
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

    setIsSubmitting(true);
    try {
      let response;

      // 게시글 신고와 댓글 신고에 따라 다른 엔드포인트 사용
      if (reportingTarget.type === ReportType.POST) {
        // 게시글 신고
        response = await fetch(`${API_BASE}/board/${postId}/report`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify(reportContent),
        });
      } else if (reportingTarget.type === ReportType.COMMENT) {
        // 댓글 신고
        response = await fetch(
          `${API_BASE}/board/${postId}/${reportingTarget.id}/report`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(reportContent),
          }
        );
      } else {
        // 기타 유형의 신고
        response = await fetch(`${API_BASE}/report`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
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
      <article className="card bg-white">
        <header className="mb-4 card bg-light">
          <h1 className="fw-bolder pt-4 pb-2 text-center">{post.title}</h1>
          <div className="fw-bold text-xl-end mx-3">{post.farmName}</div>
          <div className="text-muted fst-italic mb-2 text-xl-end mx-3">
            {formatDateTime(post.createdAt)}
          </div>
        </header>

        <section className="mb-3 px-4">
          <div
            className="fs-5 mb-4"
            dangerouslySetInnerHTML={{ __html: post.content }}
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
              <span className="text-muted">추천 ({post.likes})</span>
            )}
            <button className="btn btn-outline-secondary">
              <i className="bi bi-share"></i> 공유하기
            </button>
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
                    />
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
                onChange={(e) => setCommentContent(e.target.value)}
              />
              <div className="d-flex justify-content-end mt-2">
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
                    <div className="fw-bold">{comment.farmName}</div>
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
                      />
                      <div className="d-flex justify-content-end mt-2 gap-2">
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
                  ) : (
                    <p>{comment.content}</p>
                  )}

                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      {user && user.userId !== comment.userId && (
                        <>
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
                          <button
                            className="btn btn-sm btn-outline-warning ms-2"
                            onClick={() =>
                              handleStartReport(comment.id, ReportType.COMMENT)
                            }
                            disabled={isSubmitting}
                          >
                            <i className="bi bi-exclamation-triangle"></i> 신고
                          </button>
                        </>
                      )}
                      {user === null && (
                        <span className="text-muted small">
                          추천 ({comment.likes})
                        </span>
                      )}
                      {user &&
                        user.userId === comment.userId &&
                        editingCommentId !== comment.id && (
                          <>
                            <button
                              className="btn btn-sm btn-outline-secondary ms-2"
                              onClick={() => handleEditComment(comment)}
                            >
                              <i className="bi bi-pencil"></i> 수정
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger ms-2"
                              onClick={() => handleDeleteComment(comment.id)}
                              disabled={isSubmitting}
                            >
                              <i className="bi bi-trash"></i> 삭제
                            </button>
                          </>
                        )}
                    </div>
                  </div>

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
                                />
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
