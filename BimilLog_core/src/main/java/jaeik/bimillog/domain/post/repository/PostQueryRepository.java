package jaeik.bimillog.domain.post.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.entity.jpa.QPostLike;
import jaeik.bimillog.domain.post.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * <h2>게시글 조회 어댑터</h2>
 * <p>게시글 조회 포트의 JPA/QueryDSL 구현체입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색</p>
 * <p>MySQL 전문 검색과 QueryDSL 쿼리 처리</p>
 * <p>배치 조회로 댓글 수와 추천 수 조회</p>
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
    private static final QMember member = QMember.member;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QComment comment = QComment.comment;

    /**
     * <h3>게시판 조회</h3>
     * <p>페이지 정보에 따라 게시글 목록을 조회합니다.</p>
     * <p>공지사항은 제외하고 일반 게시글만 조회</p>
     * <p>{@link PostQueryService}에서 게시판 메인 목록 조회 시 호출됩니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findByPage(Pageable pageable, Long memberId) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(post.isNotice.isFalse());
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>{@link PostQueryService}에서 사용자 작성 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findPostsByMemberId(Long memberId, Pageable pageable, Long viewerId) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(member.id.eq(memberId));
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>사용자 추천 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 추천일 기준 최신순으로 페이지네이션 조회합니다.</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     * <p>PostQueryHelper 사용하지 않고 직접 QueryDSL 구현 (postLike.createdAt DESC 정렬)</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지 (추천일 기준 최신순)
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        QPostLike subPostLike = new QPostLike("subPostLike");
        JPQLQuery<Integer> likeCountSubQuery = JPAExpressions
                .select(subPostLike.count().intValue())
                .from(subPostLike)
                .where(subPostLike.post.id.eq(post.id));

        List<PostSimpleDetail> content = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        likeCountSubQuery,
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)  // commentCount는 배치 조회로 채움
                ))
                .from(post)
                .join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)))
                .leftJoin(post.member, member)
                .orderBy(postLike.createdAt.desc())  // 추천일 기준 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count 쿼리
        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>주간 인기 게시글 조회 (스케줄러용)</h3>
     * <p>지난 7일간의 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 주간 인기 게시글 목록 (최대 5개, PostSimpleDetail)
     */
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findWeeklyPopularPosts() {
        return findWeeklyPopularPosts(PageRequest.of(0, 5)).getContent();
    }

    /**
     * <h3>주간 인기 게시글 조회 (페이징)</h3>
     * <p>지난 7일간의 인기 게시글 목록을 페이징으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 주간 인기 게시글 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> findWeeklyPopularPosts(Pageable pageable) {
        BooleanExpression weeklyCondition = post.createdAt.after(Instant.now().minus(7, ChronoUnit.DAYS));

        Consumer<JPAQuery<?>> contentCustomizer = query -> query
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(weeklyCondition)
                .groupBy(post.id, member.id, post.title)
                .having(postLike.countDistinct().goe(1))
                .orderBy(postLike.countDistinct().desc());

        Consumer<JPAQuery<?>> countCustomizer = query -> query
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(weeklyCondition)
                .groupBy(post.id)
                .having(postLike.countDistinct().goe(1));

        return findPostsWithQuery(contentCustomizer, countCustomizer, pageable);
    }

    /**
     * <h3>레전드 게시글 조회 (스케줄러용)</h3>
     * <p>추천 수가 20개 이상인 게시글 중 가장 추천 수가 많은 상위 50개 게시글을 조회합니다.</p>
     *
     * @return 전설의 게시글 목록 (최대 50개, PostSimpleDetail)
     */
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findLegendaryPosts() {
        return findLegendaryPosts(PageRequest.of(0, 50)).getContent();
    }

    /**
     * <h3>레전드 게시글 조회 (페이징)</h3>
     * <p>추천 수가 20개 이상인 게시글을 페이징으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 레전드 게시글 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> findLegendaryPosts(Pageable pageable) {
        Consumer<JPAQuery<?>> contentCustomizer = query -> query
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id, member.id, post.title)
                .having(postLike.countDistinct().goe(20))
                .orderBy(postLike.countDistinct().desc());

        Consumer<JPAQuery<?>> countCustomizer = query -> query
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id)
                .having(postLike.countDistinct().goe(20));

        return findPostsWithQuery(contentCustomizer, countCustomizer, pageable);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>{@link PostQueryService}에서 게시글 상세 페이지 조회 시 호출됩니다.</p>
     *
     * @param postId   조회할 게시글 ID
     * @param memberId 현재 사용자 ID (좋아요 여부 확인용, null 가능)
     * @return 게시글 상세 정보 프로젝션 (게시글이 없으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    public Optional<PostDetail> findPostDetail(Long postId, Long memberId) {
        QPostLike userPostLike = new QPostLike("userPostLike");

        PostDetail result = jpaQueryFactory.select(new QPostDetail(
                        post.id,
                        post.title,
                        post.content,
                        post.views,
                        postLike.countDistinct().castToNum(Integer.class),
                        post.createdAt,
                        member.id,
                        member.memberName,
                        comment.countDistinct().castToNum(Integer.class),
                        new CaseBuilder()
                                .when(userPostLike.id.isNotNull())
                                .then(true)
                                .otherwise(false),
                        post.isNotice
                ))
                .from(post)
                .leftJoin(post.member, member)
                .leftJoin(postLike).on(postLike.post.id.eq(post.id))
                .leftJoin(comment).on(comment.post.id.eq(post.id))
                .leftJoin(userPostLike).on(
                        userPostLike.post.id.eq(post.id)
                                .and(memberId != null ? userPostLike.member.id.eq(memberId) : Expressions.FALSE)
                )
                .where(post.id.eq(postId))
                .groupBy(post.id, post.title, post.content, post.views, post.createdAt,
                        member.id, member.memberName, post.isNotice, userPostLike.id)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * <h3>공지사항 목록 조회 (스케줄러용)</h3>
     * <p>공지사항 게시글 목록을 최신순으로 조회합니다.</p>
     *
     * @return 공지사항 게시글 목록 (최대 100개, PostSimpleDetail)
     */
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findNoticePosts() {
        return findNoticePosts(PageRequest.of(0, 100)).getContent();
    }

    /**
     * <h3>공지사항 목록 조회 (페이징)</h3>
     * <p>공지사항 게시글 목록을 페이징으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 공지사항 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> findNoticePosts(Pageable pageable) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(post.isNotice.isTrue());
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>최근 인기 게시글 조회 (실시간 인기글 폴백용)</h3>
     * <p>최근 1시간 이내 생성된 게시글 중 (조회수 + 추천수*30) 기준 상위 5개를 조회합니다.</p>
     * <p>Redis 장애 시 실시간 인기글의 Graceful Degradation 폴백으로 사용됩니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 최근 인기 게시글 페이지 (PostSimpleDetail, 최대 5개)
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> findRecentPopularPosts(Pageable pageable) {
        QPostLike subPostLike = new QPostLike("subPostLike");
        JPQLQuery<Integer> likeCountSubQuery = JPAExpressions
                .select(subPostLike.count().intValue())
                .from(subPostLike)
                .where(subPostLike.post.id.eq(post.id));

        // 1시간 이내 생성된 게시글
        BooleanExpression recentCondition = post.createdAt.after(Instant.now().minus(1, ChronoUnit.HOURS));

        // 인기도 점수: 조회수 + (추천수 * 30)
        var popularityScore = post.views.add(Expressions.asNumber(likeCountSubQuery).multiply(30));

        // 상위 5개로 제한
        int maxResults = 5;
        int offset = (int) Math.min(pageable.getOffset(), maxResults);
        int limit = Math.min(pageable.getPageSize(), maxResults - offset);

        if (offset >= maxResults) {
            return new PageImpl<>(List.of(), pageable, maxResults);
        }

        List<PostSimpleDetail> content = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        likeCountSubQuery,
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)  // commentCount는 배치 조회로 채움
                ))
                .from(post)
                .leftJoin(post.member, member)
                .where(recentCondition)
                .where(post.isNotice.isFalse())
                .orderBy(popularityScore.desc(), post.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();

        // 전체 개수는 최대 5개로 제한
        Long actualTotal = jpaQueryFactory
                .select(post.count())
                .from(post)
                .where(recentCondition)
                .where(post.isNotice.isFalse())
                .fetchOne();

        long total = Math.min(actualTotal != null ? actualTotal : 0L, maxResults);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * <h3>PostId 목록으로 Post 리스트 반환</h3>
     */
    public List<Post> findAllByIds(List<Long> postIds) {
        return jpaQueryFactory
                .select(post)
                .from(post)
                .leftJoin(post.member, member).fetchJoin()
                .where(post.id.in(postIds))
                .fetch();
    }

    /**
     * <h3>PostId 목록으로 PostSimpleDetail 페이징 조회</h3>
     * <p>폴백 저장소에서 조회한 postId 목록으로 게시글 상세 정보를 페이징 조회합니다.</p>
     * <p>ID 내림차순으로 정렬됩니다.</p>
     *
     * @param postIds  조회할 게시글 ID 목록
     * @param pageable 페이징 정보
     * @return PostSimpleDetail 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostSimpleDetail> findPostSimpleDetailsByIds(List<Long> postIds, Pageable pageable) {
        if (postIds == null || postIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        QPostLike subPostLike = new QPostLike("subPostLike");
        JPQLQuery<Integer> likeCountSubQuery = JPAExpressions
                .select(subPostLike.count().intValue())
                .from(subPostLike)
                .where(subPostLike.post.id.eq(post.id));

        List<PostSimpleDetail> content = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        likeCountSubQuery,
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)))
                .from(post)
                .leftJoin(post.member, member)
                .where(post.id.in(postIds))
                .orderBy(post.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, postIds.size());
    }

    /**
     * <h3>단건 게시글 간단 상세 조회</h3>
     * <p>단일 게시글의 PostSimpleDetail을 조회합니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @return PostSimpleDetail (없으면 Optional.empty)
     */
    @Transactional(readOnly = true)
    public Optional<PostSimpleDetail> findPostSimpleDetailById(Long postId) {
        if (postId == null) {
            return Optional.empty();
        }

        QPostLike subPostLike = new QPostLike("subPostLike");
        JPQLQuery<Integer> likeCountSubQuery = JPAExpressions
                .select(subPostLike.count().intValue())
                .from(subPostLike)
                .where(subPostLike.post.id.eq(post.id));

        PostSimpleDetail result = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        likeCountSubQuery,
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)))
                .from(post)
                .leftJoin(post.member, member)
                .where(post.id.eq(postId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>배치 조회로 댓글 수는 별도 조회</p>
     *
     * @param contentQueryCustomizer Content 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param countQueryCustomizer   Count 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param pageable               페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findPostsWithQuery(Consumer<JPAQuery<?>> contentQueryCustomizer,
                                                     Consumer<JPAQuery<?>> countQueryCustomizer, Pageable pageable) {
        QPostLike subPostLike = new QPostLike("subPostLike");
        JPQLQuery<Integer> likeCountSubQuery = JPAExpressions
                .select(subPostLike.count().intValue())
                .from(subPostLike)
                .where(subPostLike.post.id.eq(post.id));

        JPAQuery<PostSimpleDetail> contentQuery = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        likeCountSubQuery,
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "익명"),
                        Expressions.constant(0)))
                .from(post)
                .leftJoin(post.member, member);

        // 커스터마이징 적용 (JOIN, WHERE 등)
        contentQueryCustomizer.accept(contentQuery);

        // 페이징 및 정렬
        List<PostSimpleDetail> content = contentQuery
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count 쿼리 빌딩
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .leftJoin(post.member, member);


        // 커스터마이징 적용 (JOIN, WHERE 등)
        countQueryCustomizer.accept(countQuery);
        Long total = countQuery.fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
