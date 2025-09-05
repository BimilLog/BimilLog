package jaeik.bimillog.domain.comment.entity;

import lombok.Builder;

/**
 * <h3>댓글 요청 값 객체</h3>
 * <p>
 * 댓글 작성/수정/삭제 요청을 담는 도메인 순수 값 객체
 * CommentReqDTO의 도메인 전용 대체
 * </p>
 * <p>
 * record 타입으로 불변성 보장 및 타입 안전성 제공
 * </p>
 *
 * @param id 댓글 ID (수정/삭제 시 사용)
 * @param parentId 부모 댓글 ID (대댓글 작성 시 사용)
 * @param postId 게시글 ID
 * @param userId 사용자 ID (로그인 사용자의 경우)
 * @param content 댓글 내용
 * @param password 댓글 비밀번호 (비회원 댓글의 경우)
 * @author Jaeik
 * @version 2.0.0
 */
public record CommentRequest(
        Long id,
        Long parentId,
        Long postId,
        Long userId,
        String content,
        Integer password
) {

    @Builder
    public CommentRequest {
    }
}