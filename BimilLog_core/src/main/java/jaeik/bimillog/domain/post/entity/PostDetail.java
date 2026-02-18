package jaeik.bimillog.domain.post.entity;

import com.querydsl.core.annotations.QueryProjection;
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
@NoArgsConstructor
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

    /**
     * <h3>생성자 - QueryDSL Projection용</h3>
     * <p>QueryDSL @QueryProjection을 위한 전용 생성자입니다.</p>
     * <p>PostQueryAdapter에서 게시글 상세 조회 시 QueryDSL을 통해 호출됩니다.</p>
     */
    @Builder
    @QueryProjection
    public PostDetail(Long id, String title, String content, Integer viewCount,
                     Integer likeCount, Instant createdAt, Long memberId,
                     String memberName, Integer commentCount, boolean isLiked,
                     boolean isWeekly, boolean isLegend, boolean isNotice) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.memberId = memberId;
        this.memberName = memberName;
        this.commentCount = commentCount;
        this.isLiked = isLiked;
        this.isWeekly = isWeekly;
        this.isLegend = isLegend;
        this.isNotice = isNotice;
    }
}