package jaeik.bimillog.domain.post.entity;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import lombok.AllArgsConstructor;
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
 * <p>레디스 캐시를 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * Post 엔티티로부터 PostSimpleDetail 생성
     */
    public static PostSimpleDetail from(Post post) {
        return PostSimpleDetail.builder()
                .id(post.getId())
                .title(post.getTitle())
                .viewCount(post.getViews())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .memberId(post.getMember() != null ? post.getMember().getId() : null)
                .memberName(post.getMemberName())
                .commentCount(post.getCommentCount())
                .isWeekly(post.isWeekly())
                .isLegend(post.isLegend())
                .isNotice(post.isNotice())
                .build();
    }
}
