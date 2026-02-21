package jaeik.bimillog.domain.post.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <h2>게시글 조회 타입</h2>
 * <p>게시글 조회 쿼리의 조건·정렬·제한 설정을 정의합니다.</p>
 * <ul>
 *   <li>WEEKLY / LEGEND / NOTICE : Redis 장애 시 DB 폴백용</li>
 *   <li>WEEKLY_SCHEDULER / LEGEND_SCHEDULER : 스케줄러 전용 (limit 보유)</li>
 *   <li>REALTIME_FALLBACK : 실시간 인기글 Redis/Caffeine 전체 장애 시 DB 폴백용</li>
 *   <li>MEMBER_POSTS : 회원 작성 게시글 조회 (memberConditionFn으로 memberId 조건 제공)</li>
 *   <li>TITLE / WRITER / TITLE_CONTENT : 검색 타입 (prefixConditionFn / partialConditionFn으로 쿼리 조건 제공)</li>
 *   <li>CAFFEINE_REALTIME : 카페인 폴백용 (idsConditionFn으로 postId 목록 조건 제공)</li>
 * </ul>
 */
@Getter
@SuppressWarnings({"rawtypes", "unchecked"})
public enum PostQueryType {

    WEEKLY(
            () -> QPost.post.isWeekly.eq(true),
            null, null, null, null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    LEGEND(
            () -> QPost.post.isLegend.eq(true),
            null, null, null, null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    NOTICE(
            () -> QPost.post.isNotice.eq(true),
            null, null, null, null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    WEEKLY_SCHEDULER(
            () -> QPost.post.createdAt.after(Instant.now().minus(7, ChronoUnit.DAYS))
                    .and(QPost.post.likeCount.goe(1)),
            null, null, null, null,
            new OrderSpecifier[]{QPost.post.likeCount.desc()}, 5),

    LEGEND_SCHEDULER(
            () -> QPost.post.likeCount.goe(20),
            null, null, null, null,
            new OrderSpecifier[]{QPost.post.likeCount.desc()}, 50),

    REALTIME_FALLBACK(
            () -> QPost.post.createdAt.after(Instant.now().minus(1, ChronoUnit.HOURS)),
            null, null, null, null,
            new OrderSpecifier[]{
                    QPost.post.views.add(QPost.post.likeCount.multiply(30)).desc(),
                    QPost.post.createdAt.desc()
            }, -1),

    MEMBER_POSTS(
            null,
            QPost.post.member.id::eq,
            null, null, null,
            new OrderSpecifier[]{QPost.post.createdAt.desc()}, -1),

    TITLE(
            null, null,
            QPost.post.title::startsWith,
            QPost.post.title::contains,
            null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    WRITER(
            null, null,
            QPost.post.memberName::startsWith,
            QPost.post.memberName::contains,
            null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    TITLE_CONTENT(
            null, null,
            query -> QPost.post.title.startsWith(query).or(QPost.post.content.startsWith(query)),
            query -> QPost.post.title.contains(query).or(QPost.post.content.contains(query)),
            null,
            new OrderSpecifier[]{QPost.post.id.desc()}, -1),

    CAFFEINE_REALTIME(
            null, null, null, null,
            ids -> QPost.post.id.in(ids),
            new OrderSpecifier[]{QPost.post.id.desc()}, -1);

    private final Supplier<BooleanExpression> conditionFn;
    private final Function<Long, BooleanExpression> memberConditionFn;
    private final Function<String, BooleanExpression> prefixConditionFn;
    private final Function<String, BooleanExpression> partialConditionFn;
    private final Function<List<Long>, BooleanExpression> idsConditionFn;
    private final OrderSpecifier[] orders;
    private final int limit;

    PostQueryType(
            Supplier<BooleanExpression> conditionFn,
            Function<Long, BooleanExpression> memberConditionFn,
            Function<String, BooleanExpression> prefixConditionFn,
            Function<String, BooleanExpression> partialConditionFn,
            Function<List<Long>, BooleanExpression> idsConditionFn,
            OrderSpecifier[] orders,
            int limit) {
        this.conditionFn = conditionFn;
        this.memberConditionFn = memberConditionFn;
        this.prefixConditionFn = prefixConditionFn;
        this.partialConditionFn = partialConditionFn;
        this.idsConditionFn = idsConditionFn;
        this.orders = orders;
        this.limit = limit;
    }

    public BooleanExpression condition() {
        return conditionFn.get();
    }

    public boolean hasLimit() {
        return limit > 0;
    }
}
