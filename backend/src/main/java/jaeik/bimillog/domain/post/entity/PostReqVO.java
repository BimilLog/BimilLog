package jaeik.bimillog.domain.post.entity;

import lombok.Builder;

/**
 * <h3>게시글 요청 값 객체</h3>
 * <p>
 * 게시글 작성/수정 요청 정보를 담는 도메인 순수 값 객체
 * PostReqDTO의 도메인 전용 대체
 * </p>
 *
 * @param title 게시글 제목
 * @param content 게시글 내용
 * @param password 게시글 비밀번호 (선택적)
 * @author Jaeik
 * @version 2.0.0
 */
public record PostReqVO(
        String title,
        String content,
        Integer password
) {

    @Builder
    public PostReqVO {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 제목은 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 내용은 필수입니다.");
        }
    }

    /**
     * <h3>게시글 요청 값 객체 생성</h3>
     * <p>제목과 내용으로 게시글 요청 정보를 생성합니다.</p>
     *
     * @param title 게시글 제목
     * @param content 게시글 내용
     * @return PostReqVO 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostReqVO of(String title, String content) {
        return PostReqVO.builder()
                .title(title)
                .content(content)
                .build();
    }

    /**
     * <h3>비밀번호 포함 게시글 요청 값 객체 생성</h3>
     * <p>제목, 내용, 비밀번호로 게시글 요청 정보를 생성합니다.</p>
     *
     * @param title 게시글 제목
     * @param content 게시글 내용
     * @param password 게시글 비밀번호
     * @return PostReqVO 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostReqVO of(String title, String content, Integer password) {
        return PostReqVO.builder()
                .title(title)
                .content(content)
                .password(password)
                .build();
    }
}