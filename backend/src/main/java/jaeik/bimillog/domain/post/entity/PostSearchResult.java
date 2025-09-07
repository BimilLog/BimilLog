package jaeik.bimillog.domain.post.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h3>게시글 검색 결과 값 객체</h3>
 * <p>
 * 게시글 검색/목록 조회 결과를 담는 도메인 전용 객체
 * SimplePostResDTO의 도메인 전용 대체
 * </p>
 * <p>
 * 성능 최적화를 위해 mutable로 변경 - 댓글수, 추천수를 나중에 설정 가능
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class PostSearchResult implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private PostCacheFlag postCacheFlag;
    private Instant createdAt;
    private Long userId;
    private String userName;
    private Integer commentCount;
    private boolean isNotice;

    /**
     * <h3>게시글 검색 결과 생성</h3>
     * <p>게시글 엔티티와 메타 정보로부터 검색 결과를 생성합니다.</p>
     * <p>PostDetail을 통해 중복 코드를 제거하고 일관성을 보장합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @param commentCount 댓글수
     * @return PostSearchResult 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostSearchResult of(Post post, Integer likeCount, Integer commentCount) {
        return PostDetail.of(post, likeCount, commentCount).toSearchResult();
    }

    /**
     * <h3>기본 댓글 수로 검색 결과 생성</h3>
     * <p>댓글 수 0으로 기본 검색 결과를 생성합니다.</p>
     * <p>PostDetail을 통해 중복 코드를 제거하고 일관성을 보장합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @return PostSearchResult 값 객체
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostSearchResult of(Post post, Integer likeCount) {
        return PostDetail.of(post, likeCount).toSearchResult();
    }

    /**
     * <h3>PostDetail에서 검색 결과 생성</h3>
     * <p>PostDetail 값 객체에서 mutable PostSearchResult를 생성합니다.</p>
     * <p>성능 최적화를 위해 나중에 댓글수, 추천수를 수정할 수 있습니다.</p>
     *
     * @param postDetail PostDetail 값 객체
     * @return PostSearchResult mutable 검색 결과
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostSearchResult ofPostDetail(PostDetail postDetail) {
        return PostSearchResult.builder()
                .id(postDetail.id())
                .title(postDetail.title())
                .content(postDetail.content())
                .viewCount(postDetail.viewCount())
                .likeCount(postDetail.likeCount())
                .postCacheFlag(postDetail.postCacheFlag())
                .createdAt(postDetail.createdAt())
                .userId(postDetail.userId())
                .userName(postDetail.userName())
                .commentCount(postDetail.commentCount())
                .isNotice(postDetail.isNotice())
                .build();
    }
    
    /**
     * <h3>생성자 - QueryDSL Projection용</h3>
     * <p>QueryDSL Projections.constructor를 위한 생성자</p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    public PostSearchResult(Long id, String title, String content, Integer viewCount, 
                           Integer likeCount, PostCacheFlag postCacheFlag, Instant createdAt,
                           Long userId, String userName, Integer commentCount, boolean isNotice) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.postCacheFlag = postCacheFlag;
        this.createdAt = createdAt;
        this.userId = userId;
        this.userName = userName;
        this.commentCount = commentCount;
        this.isNotice = isNotice;
    }
}