package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h2>간단 게시글 결과 객체</h2>
 * <p>게시글 목록 조회와 검색 결과를 담는 mutable 도메인 객체입니다.</p>
 * <p>QueryDSL Projection과 레디스 캐시를 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class PostSimpleDetail implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
    private Instant createdAt;
    private Long memberId;
    private String memberName;
    private Integer commentCount;
    private boolean isWeekly;
    private boolean isLegend;
    private boolean isNotice;

    @Builder
    @QueryProjection
    public PostSimpleDetail(Long id, String title, Integer viewCount, Integer likeCount, Instant createdAt,
                            Long memberId, String memberName, Integer commentCount,
                            boolean isWeekly, boolean isLegend, boolean isNotice) {
        this.id = id;
        this.title = title;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.memberName = memberName;
        this.commentCount = commentCount;
        this.isWeekly = isWeekly;
        this.isLegend = isLegend;
        this.isNotice = isNotice;
    }

    /**
     * 새 게시글 저장 직후 첫 페이지 캐시용 PostSimpleDetail 생성 (카운터 0으로 초기화)
     */
    public static PostSimpleDetail ofNewPost(Post post, Long memberId, String memberName) {
        return new PostSimpleDetail(post.getId(), post.getTitle(), 0, 0, post.getCreatedAt(),
                memberId, memberName, 0, false, false, false);
    }

    /**
     * Post 엔티티로부터 PostSimpleDetail 생성
     */
    public static PostSimpleDetail from(Post post) {
        return new PostSimpleDetail(
                post.getId(),
                post.getTitle(),
                post.getViews(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getMember() != null ? post.getMember().getId() : null,
                post.getMemberName(),
                post.getCommentCount(),
                post.isWeekly(),
                post.isLegend(),
                post.isNotice()
        );
    }
}