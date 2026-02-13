package jaeik.bimillog.domain.post.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <h2>JSON LIST 캐시 전용 DTO</h2>
 * <p>카운트 필드(조회수/추천수/댓글수)를 제외한 정적 필드만 포함합니다.</p>
 * <p>카운트는 {@code post:counters} Hash에서 별도 관리합니다.</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
public record PostCacheEntry(
        Long id,
        String title,
        Instant createdAt,
        Long memberId,
        String memberName,
        boolean weekly,
        boolean legend,
        boolean notice
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * PostSimpleDetail → PostCacheEntry 변환 (카운트 제거)
     */
    public static PostCacheEntry from(PostSimpleDetail detail) {
        return new PostCacheEntry(
                detail.getId(),
                detail.getTitle(),
                detail.getCreatedAt(),
                detail.getMemberId(),
                detail.getMemberName(),
                detail.isWeekly(),
                detail.isLegend(),
                detail.isNotice()
        );
    }

    /**
     * PostCacheEntry + PostCountCache → PostSimpleDetail 결합
     */
    public PostSimpleDetail toPostSimpleDetail(PostCountCache counts) {
        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(counts.viewCount())
                .likeCount(counts.likeCount())
                .commentCount(counts.commentCount())
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .isWeekly(weekly)
                .isLegend(legend)
                .isNotice(notice)
                .build();
    }
}
