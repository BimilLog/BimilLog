package jaeik.growfarm.domain.comment.entity;

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
 * @since 2.0.0
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

    /**
     * <h3>댓글 작성 요청 생성</h3>
     * <p>새 댓글 작성을 위한 요청을 생성합니다.</p>
     *
     * @param postId 게시글 ID
     * @param content 댓글 내용
     * @param password 댓글 비밀번호 (비회원의 경우)
     * @return CommentRequest 값 객체
     */
    public static CommentRequest createComment(Long postId, String content, Integer password) {
        return CommentRequest.builder()
                .postId(postId)
                .content(content)
                .password(password)
                .build();
    }

    /**
     * <h3>대댓글 작성 요청 생성</h3>
     * <p>대댓글 작성을 위한 요청을 생성합니다.</p>
     *
     * @param postId 게시글 ID
     * @param parentId 부모 댓글 ID
     * @param content 댓글 내용
     * @param password 댓글 비밀번호 (비회원의 경우)
     * @return CommentRequest 값 객체
     */
    public static CommentRequest createReply(Long postId, Long parentId, String content, Integer password) {
        return CommentRequest.builder()
                .postId(postId)
                .parentId(parentId)
                .content(content)
                .password(password)
                .build();
    }

    /**
     * <h3>댓글 수정 요청 생성</h3>
     * <p>기존 댓글 수정을 위한 요청을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param content 수정할 댓글 내용
     * @param password 댓글 비밀번호 (비회원의 경우)
     * @return CommentRequest 값 객체
     */
    public static CommentRequest updateComment(Long id, String content, Integer password) {
        return CommentRequest.builder()
                .id(id)
                .content(content)
                .password(password)
                .build();
    }

    /**
     * <h3>댓글 삭제 요청 생성</h3>
     * <p>기존 댓글 삭제를 위한 요청을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param password 댓글 비밀번호 (비회원의 경우)
     * @return CommentRequest 값 객체
     */
    public static CommentRequest deleteComment(Long id, Integer password) {
        return CommentRequest.builder()
                .id(id)
                .password(password)
                .build();
    }

    /**
     * <h3>회원 댓글 요청인지 확인</h3>
     * <p>비밀번호가 없는 경우 회원 댓글로 판단합니다.</p>
     *
     * @return 회원 댓글인 경우 true
     */
    public boolean isMemberComment() {
        return password == null;
    }

    /**
     * <h3>비회원 댓글 요청인지 확인</h3>
     * <p>비밀번호가 있는 경우 비회원 댓글로 판단합니다.</p>
     *
     * @return 비회원 댓글인 경우 true
     */
    public boolean isAnonymousComment() {
        return password != null;
    }

    /**
     * <h3>대댓글인지 확인</h3>
     * <p>부모 댓글 ID가 있는 경우 대댓글로 판단합니다.</p>
     *
     * @return 대댓글인 경우 true
     */
    public boolean isReply() {
        return parentId != null;
    }
}