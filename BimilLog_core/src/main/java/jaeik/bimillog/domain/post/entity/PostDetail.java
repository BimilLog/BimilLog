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
 * <h2>게시글 상세 정보 값 객체</h2>
 * <p>게시글 상세 조회 결과를 담는 도메인 값 객체입니다.</p>
 * <p>QueryDSL Projection과 레디스 캐시를 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Instant createdAt;
    private Long memberId;
    private String memberName;
    private Integer commentCount;
    private boolean isLiked;
    private boolean isWeekly;
    private boolean isLegend;
    private boolean isNotice;

    public static PostDetail from(Post post, boolean isLiked) {
        return PostDetail.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViews())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .memberId(post.getMember() != null ? post.getMember().getId() : null)
                .memberName(post.getMemberName())
                .commentCount(post.getCommentCount())
                .isLiked(isLiked)
                .isWeekly(post.isWeekly())
                .isLegend(post.isLegend())
                .isNotice(post.isNotice())
                .build();
    }
}