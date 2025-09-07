package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h3>게시글 상세 정보 값 객체</h3>
 * <p>
 * 게시글 상세 조회 결과를 담는 도메인 순수 값 객체
 * FullPostResDTO의 도메인 전용 대체
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
 * @param isLiked 사용자 추천 여부
 * @author Jaeik
 * @version 2.0.0
 */
public record PostDetail(
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
        boolean isNotice,
        boolean isLiked
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder
    @QueryProjection
    public PostDetail {
    }

    /**
     * <h3>게시글 상세 정보 생성</h3>
     * <p>게시글 엔티티와 메타 정보로부터 상세 정보를 생성합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @param isLiked 사용자 추천 여부
     * @return PostDetail 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount, Integer commentCount, boolean isLiked) {
        return PostDetail.builder()
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
                .isLiked(isLiked)
                .build();
    }

    /**
     * <h3>추천 여부 없는 상세 정보 생성</h3>
     * <p>로그인하지 않은 사용자용 상세 정보를 생성합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @return PostDetail 값 객체 (isLiked = false)
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount, Integer commentCount) {
        return of(post, likeCount, commentCount, false);
    }

    /**
     * <h3>기본 댓글 수로 상세 정보 생성</h3>
     * <p>댓글 수 0으로 기본 상세 정보를 생성합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @return PostDetail 값 객체 (commentCount = 0, isLiked = false)
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostDetail of(Post post, Integer likeCount) {
        return of(post, likeCount, 0, false);
    }

    /**
     * <h3>추천 여부를 변경한 새로운 PostDetail 생성</h3>
     * <p>캐시된 PostDetail의 isLiked 필드만 변경하여 새로운 객체를 생성합니다.</p>
     *
     * @param isLiked 사용자 추천 여부
     * @return PostDetail 새로운 PostDetail 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public PostDetail withIsLiked(boolean isLiked) {
        return PostDetail.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .viewCount(this.viewCount)
                .likeCount(this.likeCount)
                .postCacheFlag(this.postCacheFlag)
                .createdAt(this.createdAt)
                .userId(this.userId)
                .userName(this.userName)
                .commentCount(this.commentCount)
                .isNotice(this.isNotice)
                .isLiked(isLiked)
                .build();
    }

    /**
     * <h3>목록용 검색 결과로 변환</h3>
     * <p>PostDetail에서 PostSearchResult로 변환합니다.</p>
     * <p>isLiked 정보는 목록에서 사용되지 않으므로 제외됩니다.</p>
     *
     * @return PostSearchResult 목록용 검색 결과
     * @since 2.0.0
     * @author Jaeik
     */
    public PostSearchResult toSearchResult() {
        return PostSearchResult.ofPostDetail(this);
    }

}