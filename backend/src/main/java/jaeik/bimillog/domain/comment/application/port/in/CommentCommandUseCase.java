package jaeik.bimillog.domain.comment.application.port.in;

import jaeik.bimillog.infrastructure.adapter.in.comment.web.CommentCommandController;

/**
 * <h2>댓글 명령 유스케이스</h2>
 * <p>댓글 시스템의 모든 쓰기 작업을 정의하는 인터페이스입니다.</p>
 * <p>댓글 작성, 수정, 삭제, 추천 기능</p>
 * <p>사용자 탈퇴 시 댓글 처리, 계층형 댓글 구조 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentCommandUseCase {

    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성하고 계층 구조에 맞게 저장합니다.</p>
     * <p>로그인 사용자는 별도 인증 없이 작성, 익명 사용자는 비밀번호를 설정합니다.</p>
     * <p>{@link CommentCommandController}에서 API 요청을 처리할 때 호출합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param postId 게시글 ID
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @param content 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void writeComment(Long userId, Long postId, Long parentId, String content, Integer password);

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글의 내용을 수정합니다.</p>
     * <p>댓글 작성자만 수정 가능, 익명 댓글의 경우 비밀번호 검증합니다.</p>
     * <p>{@link CommentCommandController}에서 API 요청을 처리할 때 호출합니다.</p>
     *
     * @param commentId 수정할 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param content 새로운 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void updateComment(Long commentId, Long userId, String content, Integer password);

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제하며, 자식 댓글 존재 여부에 따라 소프트 삭제 또는 하드 삭제를 수행합니다.</p>
     * <p>자식 댓글이 있는 경우 소프트 삭제로 내용만 숨김, 없는 경우 완전 삭제합니다.</p>
     * <p>{@link CommentCommandController}에서 API 요청을 처리할 때 호출합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId, Long userId, Integer password);

    /**
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글을 비즈니스 규칙에 따라 처리합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 대한 추천을 토글 방식으로 처리합니다.</p>
     * <p>이미 추천한 댓글을 다시 누르면 취소, 추천하지 않은 댓글을 누르면 추천됩니다.</p>
     * <p>{@link CommentCommandController}에서 API 요청을 처리할 때 호출합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 사용자
     * @param commentId 추천/취소할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void likeComment(Long userId, Long commentId);

    /**
     * <h3>특정 글의 모든 댓글 삭제</h3>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteCommentsByPost(Long postId);
}
