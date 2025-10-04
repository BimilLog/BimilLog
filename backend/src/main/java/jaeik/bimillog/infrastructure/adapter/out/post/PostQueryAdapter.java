package jaeik.bimillog.infrastructure.adapter.out.post;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.member.entity.member.QMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>게시글 조회 어댑터</h2>
 * <p>게시글 조회 포트의 JPA/QueryDSL 구현체입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색</p>
 * <p>MySQL 전문 검색과 QueryDSL 쿼리 처리</p>
 * <p>배치 조회로 댓글 수와 추천 수 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostQueryAdapter implements PostQueryPort {
    private final JPAQueryFactory jpaQueryFactory;
    private final PostFulltextRepository postFullTextRepository;
    private final PostToCommentPort postToCommentPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final PostRepository postRepository;

    private static final QPost post = QPost.post;
    private static final QMember member = QMember.member;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QComment comment = QComment.comment;

    /**
     * <h3>페이지별 게시글 조회</h3>
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
    public Page<PostSearchResult> findByPage(Pageable pageable) {
        BooleanExpression condition = post.isNotice.isFalse();
        return findPostsWithCondition(condition, pageable);
    }

    /**
     * <h3>검색어로 게시글 조회</h3>
     * <p>검색 유형과 쿼리에 따라 게시글을 검색하고 페이지네이션합니다.</p>
     * <p>3글자 이상이고 writer가 아니면 FULLTEXT, 아니면 LIKE 검색</p>
     * <p>{@link PostQueryService}에서 게시글 전문 검색 처리 시 호출됩니다.</p>
     *
     * @param type     검색 유형 (title, writer, title_content)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findBySearch(PostSearchType type, String query, Pageable pageable) {
        if (shouldUseFullText(type, query)) {
            Page<PostSearchResult> fullTextResult = findPostsByFullText(type, query, pageable);
            if (!fullTextResult.isEmpty()) {
                return fullTextResult;
            }
        }

        BooleanExpression likeCondition = createLikeSearchCondition(type, query).and(post.isNotice.isFalse());
        return findPostsWithCondition(likeCondition, pageable);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>{@link PostQueryService}에서 사용자 작성 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findPostsByMemberId(Long memberId, Pageable pageable) {
        BooleanExpression condition = member.id.eq(memberId);
        return findPostsWithCondition(condition, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        QPostLike userPostLike = new QPostLike("userPostLike");
        
        // 1. 게시글 기본 정보 조회 (댓글, 추천 JOIN 제외)
        List<PostSearchResult> content = buildBasePostQueryWithoutJoins()
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.member.id.eq(memberId)))
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        populateEngagementMetrics(content);

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.member.id.eq(memberId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private boolean shouldUseFullText(PostSearchType type, String query) {
        return query.length() >= 3 && type != PostSearchType.WRITER;
    }

    private BooleanExpression createLikeSearchCondition(PostSearchType type, String query) {
        return switch (type) {
            case TITLE -> post.title.contains(query);
            case WRITER -> query.length() >= 4
                    ? member.memberName.startsWith(query)
                    : member.memberName.contains(query);
            case TITLE_CONTENT -> post.title.contains(query)
                    .or(post.content.contains(query));
        };
    }

    private Page<PostSearchResult> findPostsByFullText(PostSearchType type, String query, Pageable pageable) {
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

            List<PostSearchResult> content = mapFullTextRows(rows);
            populateEngagementMetrics(content);

            return new PageImpl<>(content, pageable, total);
        } catch (DataAccessException e) {
            log.warn("FULLTEXT 검색 중 데이터베이스 오류 - type: {}, query: {}, error: {}",
                    type, query, e.getMessage());
            return Page.empty(pageable);
        } catch (IllegalArgumentException e) {
            log.debug("FULLTEXT 검색 파라미터 오류 - type: {}, query: {}, error: {}",
                    type, query, e.getMessage());
            return Page.empty(pageable);
        }
    }

    private List<PostSearchResult> mapFullTextRows(List<Object[]> rows) {
        return rows.stream()
                .map(this::mapFullTextRow)
                .collect(Collectors.toList());
    }

    private PostSearchResult mapFullTextRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = row[1] != null ? row[1].toString() : null;
        Integer views = row[2] != null ? ((Number) row[2]).intValue() : 0;
        boolean isNotice = toBoolean(row[3]);
        PostCacheFlag cacheFlag = row[4] != null ? PostCacheFlag.valueOf(row[4].toString()) : null;
        Instant createdAt = toInstant(row[5]);
        Long memberId = row[6] != null ? ((Number) row[6]).longValue() : null;
        String memberName = row[7] != null ? row[7].toString() : null;

        return PostSearchResult.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(0)
                .postCacheFlag(cacheFlag)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(0)
                .isNotice(isNotice)
                .build();
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        return null;
    }

    private void populateEngagementMetrics(List<PostSearchResult> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PostSearchResult::getId)
                .toList();

        Map<Long, Integer> commentCounts = postToCommentPort.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = postLikeQueryPort.findLikeCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
            post.setLikeCount(likeCounts.getOrDefault(post.getId(), 0));
        });
    }

    /**
     * <h3>공통 게시글 조회 메서드</h3>
     * <p>주어진 조건에 따라 게시글을 조회하고 페이지네이션합니다.</p>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     * <p>전문검색인 경우 정확한 카운트를 위해 전용 count 메서드 사용</p>
     *
     * @param condition WHERE 조건
     * @param pageable  페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    private Page<PostSearchResult> findPostsWithCondition(BooleanExpression condition, Pageable pageable) {
        List<PostSearchResult> content = buildBasePostQueryWithoutJoins()
                .where(condition)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        populateEngagementMetrics(content);

        Long total = calculateTotalCount(condition);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>총 개수 계산 메서드</h3>
     * <p>일반 조건 검색 시 총 개수를 계산합니다.</p>
     *
     * @param condition WHERE 조건
     * @return 총 게시글 수
     */
    private Long calculateTotalCount(BooleanExpression condition) {
        return jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.member, member)
                .where(condition)
                .fetchOne();
    }

    /**
     * <h3>기본 게시글 쿼리 빌더 (JOIN 제외)</h3>
     * <p>댓글과 추천 JOIN을 제외한 기본 게시글 쿼리를 생성합니다.</p>
     * <p>댓글 수와 추천 수는 별도의 배치 조회로 처리됩니다.</p>
     *
     * @return JPAQuery<PostSearchResult>
     * @author Jaeik
     * @since 2.0.0
     */
    private JPAQuery<PostSearchResult> buildBasePostQueryWithoutJoins() {
        return jpaQueryFactory
                .select(Projections.constructor(PostSearchResult.class,
                        post.id,                           // Long id
                        post.title,                        // String title
                        post.views.coalesce(0),           // Integer viewCount
                        Expressions.constant(0),          // Integer likeCount - 나중에 설정
                        post.postCacheFlag,               // PostCacheFlag postCacheFlag
                        post.createdAt,                   // Instant createdAt
                        member.id,                        // Long memberId
                        member.memberName,                // String memberName
                        Expressions.constant(0),         // Integer commentCount - 나중에 설정
                        post.isNotice))                   // boolean isNotice
                .from(post)
                .leftJoin(post.member, member);
    }

    /**
     * <h3>게시글 상세 정보 JOIN 쿼리</h3>
     * <p>게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한 번의 JOIN 쿼리로 조회합니다.</p>
     * <p>JOIN으로 필요한 데이터를 한 번에 조회</p>
     * <p>{@link PostQueryService}에서 게시글 상세 페이지 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param memberId 현재 사용자 ID (좋아요 여부 확인용, null 가능)
     * @return 게시글 상세 정보 프로젝션 (게시글이 없으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<PostDetail> findPostDetailWithCounts(Long postId, Long memberId) {
        QPostLike userPostLike = new QPostLike("userPostLike");
        
        PostDetail result = jpaQueryFactory
                .select(new QPostDetail(
                        post.id,
                        post.title,
                        post.content,
                        post.views.coalesce(0),
                        // 좋아요 개수 (COUNT) - Integer likeCount
                        postLike.countDistinct().castToNum(Integer.class),
                        post.postCacheFlag,
                        post.createdAt,
                        member.id,
                        member.memberName,
                        // 댓글 개수 (COUNT)
                        comment.countDistinct().castToNum(Integer.class),
                        // 공지사항 여부 (boolean isNotice)
                        post.isNotice.coalesce(false),
                        // 사용자 좋아요 여부 (CASE WHEN)
                        new CaseBuilder()
                                .when(userPostLike.id.isNotNull())
                                .then(true)
                                .otherwise(false)
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
                        member.id, member.memberName, post.isNotice, post.postCacheFlag, userPostLike.id)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>게시글 ID를 기준으로 게시글 상세 정보를 조회합니다.</p>
     * <p>수정 이력: null 안전성 개선 - null postId 예외 처리 추가</p>
     *
     * @param postId 게시글 ID
     * @return 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public PostDetail findPostDetail(Long postId) {
        if (postId == null) {
            return null;
        }
        return findPostDetailWithCounts(postId, null).orElse(null);
    }

    @Override
    public List<PostDetail> findPostDetailsByIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }

        List<PostDetail> results = jpaQueryFactory
                .select(new QPostDetail(
                        post.id,
                        post.title,
                        post.content,
                        post.views.coalesce(0),
                        postLike.countDistinct().castToNum(Integer.class),
                        post.postCacheFlag,
                        post.createdAt,
                        member.id,
                        member.memberName,
                        comment.countDistinct().castToNum(Integer.class),
                        post.isNotice.coalesce(false),
                        Expressions.constant(false)
                ))
                .from(post)
                .leftJoin(post.member, member)
                .leftJoin(postLike).on(postLike.post.id.eq(post.id))
                .leftJoin(comment).on(comment.post.id.eq(post.id))
                .where(post.id.in(postIds))
                .groupBy(post.id, post.title, post.content, post.views, post.createdAt,
                        member.id, member.memberName, post.isNotice, post.postCacheFlag)
                .fetch();

        Map<Long, PostDetail> resultMap = results.stream()
                .collect(Collectors.toMap(PostDetail::id, detail -> detail, (left, right) -> left));

        return postIds.stream()
                .map(resultMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * <h3>캐시 플래그가 있는 게시글 ID 조회</h3>
     * <p>사용자가 작성한 게시글 중 캐시 플래그가 설정된 게시글의 ID만 조회합니다.</p>
     */
    @Override
    public List<Long> findCachedPostIdsByMemberId(Long memberId) {
        return postRepository.findIdsWithCacheFlagByMemberId(memberId);
    }

}
