import { renderHook, act } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { useCommentInteraction } from "@/hooks/features/post/useCommentInteraction";
import type { UseCommentInteractionParams } from "@/hooks/features/post/useCommentInteraction";
import type { Comment } from "@/types/domains/comment";
import type { Post } from "@/types/domains/post";

// useToast 모킹
const mockShowToast = vi.fn();
vi.mock("@/hooks", () => ({
  useToast: () => ({ showToast: mockShowToast }),
}));

// window.confirm 모킹
const mockConfirm = vi.fn();
Object.defineProperty(window, "confirm", {
  writable: true,
  value: mockConfirm,
});

// ── 헬퍼 ──

const createComment = (overrides: Partial<Comment> = {}): Comment => ({
  id: 1,
  postId: 10,
  memberName: "테스트유저",
  content: "테스트 댓글",
  popular: false,
  deleted: false,
  likeCount: 0,
  createdAt: "2026-01-01T00:00:00Z",
  userLike: false,
  ...overrides,
});

const createPost = (overrides: Partial<Post> = {}): Post => ({
  id: 10,
  memberId: 1,
  memberName: "테스트유저",
  title: "테스트 게시글",
  content: "게시글 내용",
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  liked: false,
  weekly: false,
  legend: false,
  notice: false,
  ...overrides,
});

const createDefaultParams = (
  overrides: Partial<UseCommentInteractionParams> = {}
): UseCommentInteractionParams => ({
  postId: "10",
  post: createPost(),
  isAuthenticated: true,
  canModify: vi.fn(() => true),
  canModifyComment: vi.fn(() => true),
  openPasswordModal: vi.fn(),
  resetPasswordModal: vi.fn(),
  modalPassword: "",
  deleteMode: null,
  targetComment: null,
  createComment: vi.fn(),
  updateComment: vi.fn(),
  deleteComment: vi.fn(),
  deletePost: vi.fn(),
  likePost: vi.fn(),
  likeComment: vi.fn(),
  ...overrides,
});

describe("useCommentInteraction", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockConfirm.mockReturnValue(true);
  });

  // ────────────────────────────────────────────
  // 1. 초기 상태
  // ────────────────────────────────────────────
  describe("초기 상태", () => {
    it("editingComment이 null이다", () => {
      const { result } = renderHook(() =>
        useCommentInteraction(createDefaultParams())
      );
      expect(result.current.editingComment).toBeNull();
    });

    it("replyingTo이 null이다", () => {
      const { result } = renderHook(() =>
        useCommentInteraction(createDefaultParams())
      );
      expect(result.current.replyingTo).toBeNull();
    });

    it("showDeleteModal이 false이다", () => {
      const { result } = renderHook(() =>
        useCommentInteraction(createDefaultParams())
      );
      expect(result.current.showDeleteModal).toBe(false);
    });

    it('passwordError가 ""이다', () => {
      const { result } = renderHook(() =>
        useCommentInteraction(createDefaultParams())
      );
      expect(result.current.passwordError).toBe("");
    });
  });

  // ────────────────────────────────────────────
  // 2. 댓글 편집 플로우
  // ────────────────────────────────────────────
  describe("댓글 편집 플로우", () => {
    describe("handleEditComment", () => {
      it("editingComment을 설정하고 editContent에 기존 내용을 대입한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ content: "기존 댓글 내용" });

        act(() => {
          result.current.handleEditComment(comment);
        });

        expect(result.current.editingComment).toEqual(comment);
        expect(result.current.editContent).toBe("기존 댓글 내용");
      });
    });

    describe("handleUpdateComment", () => {
      it("빈 내용일 때 showToast를 호출한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment();

        act(() => {
          result.current.handleEditComment(comment);
        });
        act(() => {
          result.current.setEditContent("   ");
        });
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "댓글 내용을 입력해주세요.",
        });
      });

      it("255자 초과 시 showToast를 호출한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment();

        act(() => {
          result.current.handleEditComment(comment);
        });
        act(() => {
          result.current.setEditContent("a".repeat(256));
        });
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "댓글은 최대 255자까지 입력 가능합니다.",
        });
      });

      it("익명 댓글에 비밀번호가 없으면 showToast를 호출한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const anonymousComment = createComment({
          memberName: "익명",
          content: "익명 댓글",
        });

        act(() => {
          result.current.handleEditComment(anonymousComment);
        });
        act(() => {
          result.current.setEditContent("수정된 내용");
        });
        // editPassword를 설정하지 않으면 ""이므로 비밀번호 없음
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "비밀번호를 입력해주세요.",
        });
      });

      it("익명 댓글에 잘못된 비밀번호 형식이면 showToast를 호출한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const anonymousComment = createComment({
          memberName: "익명",
          content: "익명 댓글",
        });

        act(() => {
          result.current.handleEditComment(anonymousComment);
        });
        act(() => {
          result.current.setEditContent("수정된 내용");
          result.current.setEditPassword("12"); // 4자리가 아닌 비밀번호
        });
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "비밀번호는 4자리 숫자여야 합니다.",
        });
      });

      it("정상 시 updateComment를 호출한다", () => {
        const mockUpdateComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ updateComment: mockUpdateComment })
          )
        );
        const comment = createComment({ id: 5, content: "원래 내용" });

        act(() => {
          result.current.handleEditComment(comment);
        });
        act(() => {
          result.current.setEditContent("수정된 내용");
        });
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockUpdateComment).toHaveBeenCalledWith(
          {
            commentId: 5,
            postId: 10,
            content: "수정된 내용",
            password: undefined,
          },
          expect.objectContaining({
            onSuccess: expect.any(Function),
          })
        );
      });

      it("익명 댓글 정상 수정 시 비밀번호를 포함하여 updateComment를 호출한다", () => {
        const mockUpdateComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ updateComment: mockUpdateComment })
          )
        );
        const anonymousComment = createComment({
          id: 7,
          memberName: "익명",
          content: "익명 댓글",
        });

        act(() => {
          result.current.handleEditComment(anonymousComment);
        });
        act(() => {
          result.current.setEditContent("수정된 익명 댓글");
          result.current.setEditPassword("1234");
        });
        act(() => {
          result.current.handleUpdateComment();
        });

        expect(mockUpdateComment).toHaveBeenCalledWith(
          {
            commentId: 7,
            postId: 10,
            content: "수정된 익명 댓글",
            password: 1234,
          },
          expect.objectContaining({
            onSuccess: expect.any(Function),
          })
        );
      });
    });

    describe("handleCancelEdit", () => {
      it("내용 변경이 없으면 즉시 초기화한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ content: "원래 내용" });

        act(() => {
          result.current.handleEditComment(comment);
        });
        // editContent는 "원래 내용"으로 설정됨 - 변경하지 않음
        act(() => {
          result.current.handleCancelEdit();
        });

        expect(mockConfirm).not.toHaveBeenCalled();
        expect(result.current.editingComment).toBeNull();
        expect(result.current.editContent).toBe("");
      });

      it("변경이 있으면 window.confirm을 호출한다", () => {
        mockConfirm.mockReturnValue(true);
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ content: "원래 내용" });

        act(() => {
          result.current.handleEditComment(comment);
        });
        act(() => {
          result.current.setEditContent("변경된 내용");
        });
        act(() => {
          result.current.handleCancelEdit();
        });

        expect(mockConfirm).toHaveBeenCalledWith(
          "수정 중인 내용이 있습니다. 취소하시겠습니까?"
        );
        expect(result.current.editingComment).toBeNull();
      });

      it("confirm에서 취소하면 편집 상태를 유지한다", () => {
        mockConfirm.mockReturnValue(false);
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ content: "원래 내용" });

        act(() => {
          result.current.handleEditComment(comment);
        });
        act(() => {
          result.current.setEditContent("변경된 내용");
        });
        act(() => {
          result.current.handleCancelEdit();
        });

        expect(result.current.editingComment).toEqual(comment);
        expect(result.current.editContent).toBe("변경된 내용");
      });
    });
  });

  // ────────────────────────────────────────────
  // 3. 답글 플로우
  // ────────────────────────────────────────────
  describe("답글 플로우", () => {
    describe("handleReplyTo", () => {
      it("replyingTo를 설정하고 내용을 초기화한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ id: 3 });

        act(() => {
          result.current.handleReplyTo(comment);
        });

        expect(result.current.replyingTo).toEqual(comment);
        expect(result.current.replyContent).toBe("");
      });
    });

    describe("handleSubmitReply", () => {
      it("replyingTo가 없으면 아무것도 하지 않는다", () => {
        const mockCreateComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ createComment: mockCreateComment })
          )
        );

        act(() => {
          result.current.handleSubmitReply();
        });

        expect(mockCreateComment).not.toHaveBeenCalled();
      });

      it("replyingTo가 있으면 createComment를 parentId 포함하여 호출한다", () => {
        const mockCreateComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ createComment: mockCreateComment })
          )
        );
        const parentComment = createComment({ id: 5 });

        act(() => {
          result.current.handleReplyTo(parentComment);
        });
        act(() => {
          result.current.setReplyContent("답글 내용");
        });
        act(() => {
          result.current.handleSubmitReply();
        });

        expect(mockCreateComment).toHaveBeenCalledWith(
          {
            postId: 10,
            content: "답글 내용",
            parentId: 5,
            password: undefined,
          },
          expect.objectContaining({
            onSuccess: expect.any(Function),
          })
        );
      });

      it("답글에 비밀번호가 있으면 숫자로 변환하여 전달한다", () => {
        const mockCreateComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ createComment: mockCreateComment })
          )
        );
        const parentComment = createComment({ id: 5 });

        act(() => {
          result.current.handleReplyTo(parentComment);
        });
        act(() => {
          result.current.setReplyContent("답글 내용");
          result.current.setReplyPassword("4567");
        });
        act(() => {
          result.current.handleSubmitReply();
        });

        expect(mockCreateComment).toHaveBeenCalledWith(
          {
            postId: 10,
            content: "답글 내용",
            parentId: 5,
            password: 4567,
          },
          expect.objectContaining({
            onSuccess: expect.any(Function),
          })
        );
      });
    });

    describe("handleCancelReply", () => {
      it("모든 답글 상태를 초기화한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ id: 3 });

        act(() => {
          result.current.handleReplyTo(comment);
        });
        act(() => {
          result.current.setReplyContent("답글 내용");
          result.current.setReplyPassword("1234");
        });

        expect(result.current.replyingTo).not.toBeNull();

        act(() => {
          result.current.handleCancelReply();
        });

        expect(result.current.replyingTo).toBeNull();
        expect(result.current.replyContent).toBe("");
        expect(result.current.replyPassword).toBe("");
      });
    });
  });

  // ────────────────────────────────────────────
  // 4. 좋아요 플로우
  // ────────────────────────────────────────────
  describe("좋아요 플로우", () => {
    describe("handleLikePost", () => {
      it("비인증이면 warning toast를 표시한다", () => {
        const mockLikePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              isAuthenticated: false,
              likePost: mockLikePost,
            })
          )
        );

        act(() => {
          result.current.handleLikePost();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "warning",
          message: "로그인이 필요합니다.",
        });
        expect(mockLikePost).not.toHaveBeenCalled();
      });

      it("인증이면 likePost를 호출한다", () => {
        const mockLikePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              isAuthenticated: true,
              likePost: mockLikePost,
            })
          )
        );

        act(() => {
          result.current.handleLikePost();
        });

        expect(mockShowToast).not.toHaveBeenCalled();
        expect(mockLikePost).toHaveBeenCalledWith(10);
      });
    });

    describe("handleLikeComment", () => {
      it("비인증이면 warning toast를 표시한다", () => {
        const mockLikeComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              isAuthenticated: false,
              likeComment: mockLikeComment,
            })
          )
        );
        const comment = createComment({ id: 7 });

        act(() => {
          result.current.handleLikeComment(comment);
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "warning",
          message: "로그인이 필요합니다.",
        });
        expect(mockLikeComment).not.toHaveBeenCalled();
      });

      it("인증이면 likeComment를 호출한다", () => {
        const mockLikeComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              isAuthenticated: true,
              likeComment: mockLikeComment,
            })
          )
        );
        const comment = createComment({ id: 7 });

        act(() => {
          result.current.handleLikeComment(comment);
        });

        expect(mockShowToast).not.toHaveBeenCalled();
        expect(mockLikeComment).toHaveBeenCalledWith(7);
      });
    });
  });

  // ────────────────────────────────────────────
  // 5. 삭제 플로우
  // ────────────────────────────────────────────
  describe("삭제 플로우", () => {
    describe("handleDeletePostClick", () => {
      it("canModify가 false면 error toast를 표시한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ canModify: vi.fn(() => false) })
          )
        );

        act(() => {
          result.current.handleDeletePostClick();
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "삭제 권한이 없습니다.",
        });
      });

      it("익명 게시글이면 openPasswordModal을 호출한다", () => {
        const mockOpenPasswordModal = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              post: createPost({ memberName: "익명" }),
              openPasswordModal: mockOpenPasswordModal,
            })
          )
        );

        act(() => {
          result.current.handleDeletePostClick();
        });

        expect(mockOpenPasswordModal).toHaveBeenCalledWith(
          "게시글 삭제",
          "post"
        );
      });

      it("memberName이 null인 게시글이면 openPasswordModal을 호출한다", () => {
        const mockOpenPasswordModal = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              post: createPost({ memberName: null as unknown as string }),
              openPasswordModal: mockOpenPasswordModal,
            })
          )
        );

        act(() => {
          result.current.handleDeletePostClick();
        });

        expect(mockOpenPasswordModal).toHaveBeenCalledWith(
          "게시글 삭제",
          "post"
        );
      });

      it("로그인 사용자 게시글이면 showDeleteModal을 true로 설정한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              post: createPost({ memberName: "로그인유저" }),
            })
          )
        );

        act(() => {
          result.current.handleDeletePostClick();
        });

        expect(result.current.showDeleteModal).toBe(true);
      });
    });

    describe("handleDeleteComment", () => {
      it("canModifyComment가 false면 error toast를 표시한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              canModifyComment: vi.fn(() => false),
            })
          )
        );
        const comment = createComment();

        act(() => {
          result.current.handleDeleteComment(comment);
        });

        expect(mockShowToast).toHaveBeenCalledWith({
          type: "error",
          message: "삭제 권한이 없습니다.",
        });
      });

      it("익명 댓글이면 openPasswordModal을 호출한다", () => {
        const mockOpenPasswordModal = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              openPasswordModal: mockOpenPasswordModal,
            })
          )
        );
        const anonymousComment = createComment({ memberName: "익명" });

        act(() => {
          result.current.handleDeleteComment(anonymousComment);
        });

        expect(mockOpenPasswordModal).toHaveBeenCalledWith(
          "댓글 삭제",
          "comment",
          anonymousComment
        );
      });

      it("memberName이 null인 댓글이면 openPasswordModal을 호출한다", () => {
        const mockOpenPasswordModal = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              openPasswordModal: mockOpenPasswordModal,
            })
          )
        );
        const nullNameComment = createComment({
          memberName: null as unknown as string,
        });

        act(() => {
          result.current.handleDeleteComment(nullNameComment);
        });

        expect(mockOpenPasswordModal).toHaveBeenCalledWith(
          "댓글 삭제",
          "comment",
          nullNameComment
        );
      });

      it("로그인 사용자 댓글이면 showCommentDeleteModal을 true로 설정한다", () => {
        const { result } = renderHook(() =>
          useCommentInteraction(createDefaultParams())
        );
        const comment = createComment({ memberName: "로그인유저" });

        act(() => {
          result.current.handleDeleteComment(comment);
        });

        expect(result.current.showCommentDeleteModal).toBe(true);
        expect(result.current.targetDeleteComment).toEqual(comment);
      });
    });

    describe("handleConfirmDelete", () => {
      it("deletePost를 호출한다", () => {
        const mockDeletePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ deletePost: mockDeletePost })
          )
        );

        act(() => {
          result.current.handleConfirmDelete();
        });

        expect(mockDeletePost).toHaveBeenCalledWith(
          { postId: 10 },
          expect.objectContaining({
            onSuccess: expect.any(Function),
            onError: expect.any(Function),
          })
        );
      });
    });

    describe("handleConfirmCommentDelete", () => {
      it("targetDeleteComment이 없으면 아무것도 하지 않는다", () => {
        const mockDeleteComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ deleteComment: mockDeleteComment })
          )
        );

        act(() => {
          result.current.handleConfirmCommentDelete();
        });

        expect(mockDeleteComment).not.toHaveBeenCalled();
      });

      it("targetDeleteComment이 있으면 deleteComment를 호출한다", () => {
        const mockDeleteComment = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({ deleteComment: mockDeleteComment })
          )
        );
        const comment = createComment({ id: 9, memberName: "로그인유저" });

        // 먼저 삭제 대상 설정
        act(() => {
          result.current.handleDeleteComment(comment);
        });
        act(() => {
          result.current.handleConfirmCommentDelete();
        });

        expect(mockDeleteComment).toHaveBeenCalledWith(
          { commentId: 9, postId: 10 },
          expect.objectContaining({
            onSuccess: expect.any(Function),
            onError: expect.any(Function),
          })
        );
      });
    });

    describe("handlePasswordSubmit", () => {
      it("deleteMode가 'post'면 비밀번호 포함하여 deletePost를 호출한다", () => {
        const mockDeletePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              deleteMode: "post",
              modalPassword: "1234",
              deletePost: mockDeletePost,
            })
          )
        );

        act(() => {
          result.current.handlePasswordSubmit();
        });

        expect(mockDeletePost).toHaveBeenCalledWith(
          { postId: 10, password: 1234 },
          expect.objectContaining({
            onSuccess: expect.any(Function),
            onError: expect.any(Function),
          })
        );
      });

      it("deleteMode가 'comment'이고 targetComment이 있으면 deleteComment를 호출한다", () => {
        const mockDeleteComment = vi.fn();
        const targetComment = createComment({ id: 15 });
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              deleteMode: "comment",
              modalPassword: "5678",
              targetComment,
              deleteComment: mockDeleteComment,
            })
          )
        );

        act(() => {
          result.current.handlePasswordSubmit();
        });

        expect(mockDeleteComment).toHaveBeenCalledWith(
          { commentId: 15, postId: 10, password: 5678 },
          expect.objectContaining({
            onSuccess: expect.any(Function),
            onError: expect.any(Function),
          })
        );
      });

      it("onError 콜백이 호출되면 passwordError를 설정한다", () => {
        const mockDeletePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              deleteMode: "post",
              modalPassword: "1234",
              deletePost: mockDeletePost,
            })
          )
        );

        act(() => {
          result.current.handlePasswordSubmit();
        });

        // onError 콜백 추출 후 실행
        const onError = mockDeletePost.mock.calls[0][1].onError;
        act(() => {
          onError("비밀번호가 틀렸습니다.");
        });

        expect(result.current.passwordError).toBe("비밀번호가 틀렸습니다.");
      });

      it("onError에 빈 문자열이 오면 기본 에러 메시지를 설정한다", () => {
        const mockDeletePost = vi.fn();
        const { result } = renderHook(() =>
          useCommentInteraction(
            createDefaultParams({
              deleteMode: "post",
              modalPassword: "1234",
              deletePost: mockDeletePost,
            })
          )
        );

        act(() => {
          result.current.handlePasswordSubmit();
        });

        const onError = mockDeletePost.mock.calls[0][1].onError;
        act(() => {
          onError("");
        });

        expect(result.current.passwordError).toBe(
          "비밀번호가 올바르지 않습니다."
        );
      });
    });
  });
});
