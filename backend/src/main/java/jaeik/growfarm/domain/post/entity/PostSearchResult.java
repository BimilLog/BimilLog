package jaeik.growfarm.domain.post.entity;

import lombok.Builder;
import java.time.Instant;

/**
 * <h3>게시글 검색 결과 값 객체</h3>
 * <p>
 * 게시글 검색/목록 조회 결과를 담는 도메인 순수 값 객체
 * SimplePostResDTO의 도메인 전용 대체
 * </p>
 *
 * @param id 게시글 ID
 * @param title 제목
 * @param content 내용
 * @param viewCount 조회수
 * @param likeCount 추천수
 * @param postCacheFlag 캐시 플래그
 * @param createdAt 작성일시
 * @param userId 작성자 ID
 * @param userName 작성자명
 * @param commentCount 댓글수
 * @param isNotice 공지여부
 * @author Jaeik
 * @since 2.0.0
 */
public record PostSearchResult(
        Long id,
        String title,
        String content,
        Integer viewCount,
        Integer likeCount,
        PostCacheFlag postCacheFlag,
        Instant createdAt,
        Long userId,
        String userName,
        Integer commentCount,
        boolean isNotice
) {

    @Builder
    public PostSearchResult {
    }

    /**
     * <h3>게시글 검색 결과 생성</h3>
     * <p>게시글 엔티티와 메타 정보로부터 검색 결과를 생성합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @return PostSearchResult 값 객체
     */
    public static PostSearchResult of(Post post, Integer likeCount, Integer commentCount) {
        return PostSearchResult.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViews())
                .likeCount(likeCount)
                .postCacheFlag(post.getPostCacheFlag())
                .createdAt(post.getCreatedAt())
                .userId(post.getUser().getId())
                .userName(post.getUser().getUserName())
                .commentCount(commentCount)
                .isNotice(post.isNotice())
                .build();
    }

    /**
     * <h3>기본 댓글 수로 검색 결과 생성</h3>
     * <p>댓글 수 0으로 기본 검색 결과를 생성합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @return PostSearchResult 값 객체
     */
    public static PostSearchResult of(Post post, Integer likeCount) {
        return of(post, likeCount, 0);
    }
}