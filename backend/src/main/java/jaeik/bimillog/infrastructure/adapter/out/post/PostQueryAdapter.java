package jaeik.bimillog.infrastructure.adapter.out.post;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostToCommentPort;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.*;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private final PostToCommentPort postToCommentPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final PostRepository postRepository;

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
    @Override
    public Page<PostSimpleDetail> findByPage(Pageable pageable) {
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
    @Override
    public Page<PostSimpleDetail> findPostsByMemberId(Long memberId, Pageable pageable) {
        Consumer<JPAQuery<?>> customizer = query -> query.where(member.id.eq(memberId));
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSimpleDetail> findLikedPostsByMemberId(Long memberId, Pageable pageable) {
        Consumer<JPAQuery<?>> customizer = query ->
                query.join(postLike).on(post.id.eq(postLike.post.id).and(postLike.member.id.eq(memberId)));
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>공통 게시글 목록 조회</h3>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     *
     * @param contentQueryCustomizer Content 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param countQueryCustomizer   Count 쿼리 커스터마이징 로직 (JOIN, WHERE 등)
     * @param pageable               페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    private Page<PostSimpleDetail> findPostsWithQuery(Consumer<JPAQuery<?>> contentQueryCustomizer,
                                                      Consumer<JPAQuery<?>> countQueryCustomizer,
                                                      Pageable pageable) {
        JPAQuery<PostSimpleDetail> contentQuery = jpaQueryFactory
                .select(new QPostSimpleDetail(
                        post.id,
                        post.title,
                        post.views,
                        Expressions.constant(0),
                        post.createdAt,
                        member.id,
                        Expressions.stringTemplate("COALESCE({0}, {1})", member.memberName, "비회원"),
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

        // 배치 조회로 댓글 수와 추천 수 설정
        batchLikeAndCommentCount(content);

        // Count 쿼리 빌딩
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(post.countDistinct())
                .from(post);

        // 커스터마이징 적용 (JOIN, WHERE 등)
        countQueryCustomizer.accept(countQuery);

        Long total = countQuery.fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>게시글 목록에 추천 수와 댓글 수 주입</h3>
     * <p>게시글 목록의 좋아요 수와 댓글 수를 배치로 조회하여 주입.</p>
     *
     * @param posts 좋아요 수와 댓글 수를 채울 게시글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private void batchLikeAndCommentCount(List<PostSimpleDetail> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PostSimpleDetail::getId)
                .toList();

        Map<Long, Integer> commentCounts = postToCommentPort.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = postLikeQueryPort.findLikeCountsByPostIds(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
            post.setLikeCount(likeCounts.getOrDefault(post.getId(), 0));
        });
    }

    /**
     * <h3>게시글 상세 정보 JOIN 쿼리</h3>
     * <p>게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한 번의 JOIN 쿼리로 조회합니다.</p>
     * <p>JOIN으로 필요한 데이터를 한 번에 조회</p>
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
                        member.id, member.memberName, userPostLike.id)
                .fetchOne();

        return Optional.ofNullable(result);
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
    public Page<PostSimpleDetail> findBySearch(PostSearchType type, String query, Pageable pageable) {
        if (shouldUseFullText(type, query)) {
            Page<PostSimpleDetail> fullTextResult = findPostsByFullText(type, query, pageable);
            if (!fullTextResult.isEmpty()) {
                return fullTextResult;
            }
        }

        BooleanExpression likeCondition = createLikeSearchCondition(type, query).and(post.isNotice.isFalse());
        Consumer<JPAQuery<?>> customizer = q -> q.where(likeCondition);
        return findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>전문 검색 사용 여부 판단</h3>
     * <p>검색 조건에 따라 MySQL FULLTEXT 검색을 사용할지 결정합니다.</p>
     * <p>검색어가 3글자 이상이고 작성자 검색이 아닌 경우 FULLTEXT 검색을 사용합니다.</p>
     *
     * @param type  검색 유형
     * @param query 검색어
     * @return FULLTEXT 검색 사용 여부
     */
    private boolean shouldUseFullText(PostSearchType type, String query) {
        return query.length() >= 3 && type != PostSearchType.WRITER;
    }

    /**
     * <h3>LIKE 검색 조건 생성</h3>
     * <p>검색 유형에 따라 QueryDSL LIKE 검색 조건을 생성합니다.</p>
     * <p>WRITER 검색 시: 4글자 이상이면 startsWith, 미만이면 contains 사용</p>
     *
     * @param type  검색 유형
     * @param query 검색어
     * @return QueryDSL Boolean 표현식
     */
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

    /**
     * <h3>MySQL FULLTEXT 검색 실행</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 게시글을 검색합니다.</p>
     * <p>검색 실패 시 빈 페이지를 반환하며, 에러는 로그로 기록됩니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     */
    private Page<PostSimpleDetail> findPostsByFullText(PostSearchType type, String query, Pageable pageable) {
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

            List<PostSimpleDetail> content = mapFullTextRows(rows);
            batchLikeAndCommentCount(content);

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

    /**
     * <h3>FULLTEXT 검색 결과 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열 목록을 PostSimpleDetail 목록으로 변환합니다.</p>
     *
     * @param rows FULLTEXT 검색 결과 행 목록
     * @return 변환된 게시글 간략 정보 목록
     */
    private List<PostSimpleDetail> mapFullTextRows(List<Object[]> rows) {
        return rows.stream()
                .map(this::mapFullTextRow)
                .collect(Collectors.toList());
    }

    /**
     * <h3>FULLTEXT 검색 단일 행 매핑</h3>
     * <p>FULLTEXT 검색으로 조회한 Object[] 배열을 PostSimpleDetail 객체로 변환합니다.</p>
     * <p>좋아요 수와 댓글 수는 0으로 초기화되며, 이후 배치 조회로 채워집니다.</p>
     *
     * @param row FULLTEXT 검색 결과 행 (id, title, views, ?, createdAt, memberId, memberName)
     * @return 변환된 게시글 간략 정보
     */
    private PostSimpleDetail mapFullTextRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = row[1] != null ? row[1].toString() : null;
        Integer views = row[2] != null ? ((Number) row[2]).intValue() : 0;
        Instant createdAt = toInstant(row[4]);
        Long memberId = row[5] != null ? ((Number) row[5]).longValue() : null;
        String memberName = row[6] != null ? row[6].toString() : null;

        return PostSimpleDetail.builder()
                .id(id)
                .title(title)
                .viewCount(views)
                .likeCount(0)
                .createdAt(createdAt)
                .memberId(memberId)
                .memberName(memberName)
                .commentCount(0)
                .build();
    }

    /**
     * <h3>Object를 Instant로 변환</h3>
     * <p>다양한 날짜/시간 타입을 Instant로 변환합니다.</p>
     * <p>지원 타입: Instant, Timestamp, LocalDateTime</p>
     *
     * @param value 변환할 날짜/시간 객체
     * @return Instant 객체 (변환 실패 시 null)
     */
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


    /**
     * <h3>사용자가 작성한 postId 조회</h3>
     * <p>사용자가 작성한 글의 postId 목록을 조회합니다.</p>
     *
     * @param memberId 게시글을 조회할 사용자 ID
     * @return List<Long> 사용자의 게시글 ID 목록
     */
    @Override
    public List<Long> findPostIdsMemberId(Long memberId) {
        return postRepository.findIdsWithCacheFlagByMemberId(memberId);
    }

}
