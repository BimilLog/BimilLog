package jaeik.bimillog.domain.post.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h2>게시글 검색 결과 값 객체</h2>
 * <p>게시글 목록 조회와 검색 결과를 담는 mutable 도메인 객체입니다.</p>
 * <p>PostDetail과 다르게 mutable로 설계되어 대량 데이터 조회 시 메타데이터 업데이트가 가능합니다.</p>
 * <p>QueryDSL Projection과 레디스 캐시를 지원합니다.</p>
 * <p>SimplePostResDTO의 도메인 전용 대체 객체로 사용됩니다.</p>
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
    private Long memberId;
    private String memberName;
    private Integer commentCount;
    private boolean isNotice;

    /**
     * <h3>게시글 검색 결과 생성</h3>
     * <p>게시글 엔티티와 메타 정보로부터 검색 결과를 생성합니다.</p>
     * <p>PostQueryService에서 게시글 목록 조회 시 호출됩니다.</p>
     * <p>PostDetail을 경유하여 생성로직의 일관성과 코드 재사용성을 보장합니다.</p>
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
     * <p>캐시된 데이터나 댓글 수 업데이트 전 게시글에 사용됩니다.</p>
     * <p>PostDetail을 경유하여 일관된 객체 생성 로직을 유지합니다.</p>
     *
     * @param post 게시글 엔티티
     * @param likeCount 추천수
     * @return PostSearchResult 값 객체 (commentCount = 0)
     * @since 2.0.0
     * @author Jaeik
     */
    public static PostSearchResult of(Post post, Integer likeCount) {
        return PostDetail.of(post, likeCount).toSearchResult();
    }

    /**
     * <h3>PostDetail에서 검색 결과 생성</h3>
     * <p>PostDetail 값 객체에서 mutable PostSearchResult를 생성합니다.</p>
     * <p>PostDetail.toSearchResult()에서 내부적으로 호출되어 목록용 객체로 변환합니다.</p>
     * <p>isLiked 정보는 상세 조회에만 필요하므로 목록용에서는 제외됩니다.</p>
     * <p>mutable 객체로 설계되어 대량 조회 시 메타데이터 업데이트가 효율적입니다.</p>
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
                .memberId(postDetail.memberId())
                .memberName(postDetail.memberName())
                .commentCount(postDetail.commentCount())
                .isNotice(postDetail.isNotice())
                .build();
    }
    
    /**
     * <h3>생성자 - QueryDSL Projection용</h3>
     * <p>QueryDSL Projections.constructor를 위한 전용 생성자입니다.</p>
     * <p>PostQueryRepository에서 게시글 목록 조회 시 QueryDSL을 통해 호출됩니다.</p>
     * <p>DB에서 직접 PostSearchResult 객체로 조회하여 JOIN으로 한 번에 데이터를 가져옵니다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     */
    public PostSearchResult(Long id, String title, String content, Integer viewCount,
                           Integer likeCount, PostCacheFlag postCacheFlag, Instant createdAt,
                           Long memberId, String memberName, Integer commentCount, boolean isNotice) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.postCacheFlag = postCacheFlag;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.memberName = memberName;
        this.commentCount = commentCount;
        this.isNotice = isNotice;
    }
}