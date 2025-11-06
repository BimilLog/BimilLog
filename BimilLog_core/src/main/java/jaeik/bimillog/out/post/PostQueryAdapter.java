package jaeik.bimillog.out.post;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
public class PostQueryAdapter implements PostQueryPort {
    private final JPAQueryFactory jpaQueryFactory;
    private final PostFulltextRepository postFullTextRepository;
    private final PostQueryHelper postQueryHelper;
    private final PostRepository postRepository;

    private static final QPost post = QPost.post;
    private static final QMember member = QMember.member;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QComment comment = QComment.comment;

    /**
     * <h3>사용자가 작성한 글의 postId 목록 조회</h3>
     * <p>사용자가 작성한 글의 postId 목록을 조회합니다.</p>
     *
     * @param memberId 게시글을 조회할 사용자 ID
     * @return List<Long> 사용자의 게시글 ID 목록
     */
    @Override
    public List<Long> findPostIdsMemberId(Long memberId) {
        return postRepository.findIdsWithCacheFlagByMemberId(memberId);
    }

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
    @Override
    public Page<PostSimpleDetail> findByPage(Pageable pageable) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(post.isNotice.isFalse());
        return postQueryHelper.findPostsWithQuery(customizer, customizer, pageable);
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
    @Override
    public Page<PostSimpleDetail> findPostsByMemberId(Long memberId, Pageable pageable) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(member.id.eq(memberId));
        return postQueryHelper.findPostsWithQuery(customizer, customizer, pageable);
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
    @Override
    public Page<PostSimpleDetail> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        // Content 쿼리: 추천한 게시글 조회 (postLike.createdAt 기준 정렬)
        List<PostSimpleDetail> content = jpaQueryFactory
            .select(new QPostSimpleDetail(
                post.id,
                post.title,
                post.views,
                Expressions.constant(0),  // likeCount는 배치 조회로 채움
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

        // 배치 조회로 댓글 수와 추천 수 설정
        postQueryHelper.batchLikeAndCommentCount(content);

        // Count 쿼리
        Long total = jpaQueryFactory
            .select(post.countDistinct())
            .from(post)
            .join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)))
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>주간 인기 게시글 조회</h3>
     * <p>지난 7일간의 인기 게시글 목록을 조회합니다.</p>
     *
     * @return 주간 인기 게시글 목록 (최대 5개, PostSimpleDetail)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findWeeklyPopularPosts() {
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

        return postQueryHelper.findPostsWithQuery(contentCustomizer, countCustomizer, PageRequest.of(0, 5))
            .getContent();
    }

    /**
     * <h3>레전드 게시글 조회</h3>
     * <p>추천 수가 20개 이상인 게시글 중 가장 추천 수가 많은 상위 50개 게시글을 조회합니다.</p>
     *
     * @return 전설의 게시글 목록 (최대 50개, PostSimpleDetail)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<PostSimpleDetail> findLegendaryPosts() {
        Consumer<JPAQuery<?>> contentCustomizer = query -> query
            .leftJoin(postLike).on(post.id.eq(postLike.post.id))
            .groupBy(post.id, member.id, post.title)
            .having(postLike.countDistinct().goe(20))
            .orderBy(postLike.countDistinct().desc());

        Consumer<JPAQuery<?>> countCustomizer = query -> query
            .leftJoin(postLike).on(post.id.eq(postLike.post.id))
            .groupBy(post.id)
            .having(postLike.countDistinct().goe(20));

        return postQueryHelper.findPostsWithQuery(contentCustomizer, countCustomizer, PageRequest.of(0, 50))
            .getContent();
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
    @Override
    public Optional<PostDetail> findPostDetailWithCounts(Long postId, Long memberId) {
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
     * <h3>MySQL FULLTEXT 전문 검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 게시글을 검색합니다.</p>
     * <p>검색 실패 시 빈 페이지를 반환하며, 에러는 로그로 기록됩니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형 (TITLE, TITLE_CONTENT)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> findByFullTextSearch(PostSearchType type, String query, Pageable pageable) {
        String searchTerm = query + "*";
        try {
            List<Object[]> rows = switch (type) {
                case TITLE -> postFullTextRepository.findByTitleFullText(searchTerm, pageable);
                case TITLE_CONTENT -> postFullTextRepository.findByTitleContentFullText(searchTerm, pageable);
                case WRITER -> List.of();
            };

            long total = switch (type) {
                case TITLE -> postFullTextRepository.countByTitleFullText(searchTerm);
                case TITLE_CONTENT -> postFullTextRepository.countByTitleContentFullText(searchTerm);
                case WRITER -> 0L;
            };

            if (rows.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, total);
            }

            List<PostSimpleDetail> content = postQueryHelper.mapFullTextRows(rows);
            postQueryHelper.batchLikeAndCommentCount(content);

            return new PageImpl<>(content, pageable, total);
        } catch (DataAccessException e) {
            log.warn("FULLTEXT 검색 중 데이터베이스 오류 - type: {}, query: {}, error: {}", type, query, e.getMessage());
            return Page.empty(pageable);
        } catch (IllegalArgumentException e) {
            log.debug("FULLTEXT 검색 파라미터 오류 - type: {}, query: {}, error: {}", type, query, e.getMessage());
            return Page.empty(pageable);
        }
    }

    /**
     * <h3>접두사 검색 (인덱스 활용)</h3>
     * <p>LIKE 'query%' 조건으로 검색하여 인덱스를 활용합니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> findByPrefixMatch(PostSearchType type, String query, Pageable pageable) {
        BooleanExpression condition = switch (type) {
            case WRITER -> member.memberName.startsWith(query);
            case TITLE -> post.title.startsWith(query);
            case TITLE_CONTENT -> post.title.startsWith(query).or(post.content.startsWith(query));
        };

        BooleanExpression finalCondition = condition.and(post.isNotice.isFalse());
        Consumer<JPAQuery<?>> customizer = q -> q.where(finalCondition);
        return postQueryHelper.findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>부분 문자열 검색 (인덱스 미활용)</h3>
     * <p>LIKE '%query%' 조건으로 부분 검색을 수행합니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> findByPartialMatch(PostSearchType type, String query, Pageable pageable) {
        BooleanExpression condition = switch (type) {
            case TITLE -> post.title.contains(query);
            case WRITER -> member.memberName.contains(query);
            case TITLE_CONTENT -> post.title.contains(query).or(post.content.contains(query));
        };

        BooleanExpression finalCondition = condition.and(post.isNotice.isFalse());
        Consumer<JPAQuery<?>> customizer = q -> q.where(finalCondition);
        return postQueryHelper.findPostsWithQuery(customizer, customizer, pageable);
    }
}
