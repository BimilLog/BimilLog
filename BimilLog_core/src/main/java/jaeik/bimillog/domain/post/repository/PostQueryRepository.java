package jaeik.bimillog.domain.post.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.entity.jpa.QPostLike;
import jaeik.bimillog.domain.post.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <h2>게시글 조회 어댑터</h2>
 * <p>게시글 조회 포트의 JPA/QueryDSL 구현체입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색</p>
 * <p>MySQL 전문 검색과 QueryDSL 쿼리 처리</p>
 * <p>비정규화 컬럼(likeCount, commentCount, memberName) 직접 참조</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private static final QPost post = QPost.post;
    private static final QPostLike postLike = QPostLike.postLike;

    /**
     * <h3>게시판 게시글 조회 (Cursor 기반)</h3>
     * <p>커서 기반 페이지네이션으로 게시글 목록을 조회합니다.</p>
     * <p>hasNext 판단을 위해 size + 1개를 조회합니다.</p>
     *
     * @param cursor 마지막으로 조회한 게시글 ID (null이면 처음부터)
     * @param size   조회할 개수
     * @return 게시글 목록 (size + 1개까지 조회됨)
     */
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findBoardPostsByCursor(Long cursor, int size) {
        BooleanExpression cursorCondition = cursor != null ? post.id.lt(cursor) : null;
        return jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        post.likeCount,
                        post.createdAt,
                        post.member.id,
                        post.memberName,
                        post.commentCount,
                        post.isWeekly,
                        post.isLegend,
                        post.isNotice))
                .from(post)
                .where(cursorCondition)
                .orderBy(post.id.desc())
                .limit(size + 1)
                .fetch();
    }

    /**
     * <h3>PostSimpleDetail 공통 조회</h3>
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> selectPostSimpleDetails(BooleanExpression condition, Pageable pageable, OrderSpecifier<?>... orders) {
        List<PostSimpleDetail> content = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        post.likeCount,
                        post.createdAt,
                        post.member.id,
                        post.memberName,
                        post.commentCount,
                        post.isWeekly,
                        post.isLegend,
                        post.isNotice))
                .from(post)
                .where(condition)
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>사용자 추천 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 추천일 기준 최신순으로 페이지네이션 조회합니다.</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지 (추천일 기준 최신순)
     */
    public Page<PostSimpleDetail> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        List<PostSimpleDetail> content = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        post.likeCount,
                        post.createdAt,
                        post.member.id,
                        post.memberName,
                        post.commentCount,
                        post.isWeekly,
                        post.isLegend,
                        post.isNotice
                ))
                .from(post)
                .join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)))
                .orderBy(postLike.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>카운트 필드 세트 기반 벌크 증감</h3>
     * <p>CASE WHEN을 사용하여 단일 UPDATE SQL로 여러 게시글의 카운트를 일괄 증감합니다.</p>
     * <pre>
     * UPDATE post
     * SET views = views + CASE id WHEN 1 THEN 3 WHEN 2 THEN 5 ELSE 0 END
     * WHERE id IN (1, 2)
     * </pre>
     */
    public void bulkIncrementCount(Map<Long, Long> counts, NumberPath<Integer> field) {
        QPost post = QPost.post;

        CaseBuilder.Cases<Integer, NumberExpression<Integer>> caseExpression = null;
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            if (caseExpression == null) {
                caseExpression = new CaseBuilder()
                        .when(post.id.eq(entry.getKey())).then(entry.getValue().intValue());
            } else {
                caseExpression = caseExpression
                        .when(post.id.eq(entry.getKey())).then(entry.getValue().intValue());
            }
        }

        NumberExpression<Integer> delta = Objects.requireNonNull(caseExpression).otherwise(0);

        jpaQueryFactory.update(post)
                .set(field, field.add(delta))
                .where(post.id.in(counts.keySet()))
                .execute();
    }
}
