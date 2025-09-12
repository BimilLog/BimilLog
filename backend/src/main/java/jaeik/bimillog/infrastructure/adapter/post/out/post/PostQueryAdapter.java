package jaeik.bimillog.infrastructure.adapter.post.out.post;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.comment.entity.QComment;
import jaeik.bimillog.domain.post.application.port.out.PostCommentToPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.QUser;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostFulltextRepository;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final PostRepository postRepository;
    private final PostFulltextRepository postFullTextRepository;
    private final PostCommentToPort postCommentToPort;
    private final PostLikeQueryPort postLikeQueryPort;

    private static final QPost post = QPost.post;
    private static final QUser user = QUser.user;
    private static final QPostLike postLike = QPostLike.postLike;
    private static final QComment comment = QComment.comment;

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     * <p>{@link PostQueryService}에서 게시글 존재성 검증 및 권한 확인 시 호출됩니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
    }

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
    public Page<PostSearchResult> findBySearch(String type, String query, Pageable pageable) {
        BooleanExpression condition = createSearchCondition(type, query, pageable);
        return findPostsWithCondition(condition, pageable);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>{@link PostQueryService}에서 사용자 작성 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findPostsByUserId(Long userId, Pageable pageable) {
        BooleanExpression condition = user.id.eq(userId);
        return findPostsWithCondition(condition, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     * <p>{@link PostQueryService}에서 사용자 추천 게시글 내역 조회 시 호출됩니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 추천한 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPostLike userPostLike = new QPostLike("userPostLike");
        
        // 1. 게시글 기본 정보 조회 (댓글, 추천 JOIN 제외)
        List<PostSearchResult> content = buildBasePostQueryWithoutJoins()
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(PostSearchResult::getId)
                .toList();

        // 3. 배치로 댓글 수와 추천 수 조회
        Map<Long, Integer> commentCounts = postCommentToPort.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = postLikeQueryPort.findLikeCountsByPostIds(postIds);

        // 4. 댓글 수와 추천 수 설정 - mutable 객체이므로 직접 수정
        content.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
            post.setLikeCount(likeCounts.getOrDefault(post.getId(), 0));
        });

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>단순화된 검색 조건 생성</h3>
     * <p>Strategy Pattern 제거 후 직접 구현한 단순한 검색 로직</p>
     * <p>규칙: 3글자 이상이고 writer가 아니면 FULLTEXT, 아니면 LIKE</p>
     *
     * @param type  검색 유형 (title, writer, title_content)
     * @param query 검색어 (DTO에서 이미 검증됨)
     * @return 생성된 BooleanExpression
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createSearchCondition(String type, String query, Pageable pageable) {
        // 3글자 이상이고 writer가 아니면 FULLTEXT 검색 시도
        boolean shouldTryFullText = query.length() >= 3 && !"writer".equals(type);
        
        if (shouldTryFullText) {
            List<Long> postIds = getPostIdsByFullTextSearch(type, query, pageable);
            if (!postIds.isEmpty()) {
                return post.id.in(postIds).and(post.isNotice.isFalse());
            }
            // FULLTEXT 결과가 없으면 LIKE로 fallback
        }
        
        // LIKE 검색
        BooleanExpression likeCondition = switch (type) {
            case "title" -> post.title.contains(query);
            case "writer" -> query.length() >= 4 
                ? user.userName.startsWith(query)  // 4글자 이상: LIKE% 검색 (인덱스 효율성)
                : user.userName.contains(query);   // 1-3글자: %LIKE% 검색 (완전 일치)
            case "title_content" -> post.title.contains(query)
                                   .or(post.content.contains(query));
            default -> post.title.contains(query); // DTO 검증으로 인해 도달할 수 없는 분기
        };
        
        return likeCondition.and(post.isNotice.isFalse());
    }

    /**
     * <h3>FULLTEXT 검색으로 Post ID 목록 조회</h3>
     * <p>PostFullTextRepository를 사용하여 네이티브 쿼리로 검색합니다.</p>
     * <p>페이징을 적용하여 검색 결과를 제한합니다.</p>
     * 
     * @param type 검색 유형
     * @param query 검색어
     * @param pageable 페이지 정보 (검색 결과 제한용)
     * @return 검색된 Post ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getPostIdsByFullTextSearch(String type, String query, Pageable pageable) {
        try {
            String searchTerm = query + "*";
            
            // 페이징 정보를 활용하여 필요한 만큼만 조회
            // 최대 1000개로 제한하여 메모리 사용량 제한
            int limit = Math.min(pageable.getPageSize() * 10, 1000);
            Pageable searchPageable = Pageable.ofSize(limit);
            
            List<Object[]> results = switch (type) {
                case "title" -> postFullTextRepository.findByTitleFullText(searchTerm, searchPageable);
                case "title_content" -> postFullTextRepository.findByTitleContentFullText(searchTerm, searchPageable);
                default -> List.of();
            };
            
            return results.stream()
                    .map(row -> ((Number) row[0]).longValue())
                    .toList();
                    
        } catch (org.springframework.dao.DataAccessException e) {
            // 데이터베이스 관련 예외만 처리
            log.warn("FULLTEXT 검색 중 데이터베이스 오류 - type: {}, query: {}, error: {}", 
                    type, query, e.getMessage());
            return List.of(); // 빈 목록 반환하여 LIKE 검색으로 폴백
        } catch (IllegalArgumentException e) {
            // 잘못된 검색어 형식 등의 예외 처리
            log.debug("FULLTEXT 검색 파라미터 오류 - type: {}, query: {}, error: {}", 
                    type, query, e.getMessage());
            return List.of();
        }
    }

    /**
     * <h3>공통 게시글 조회 메서드</h3>
     * <p>주어진 조건에 따라 게시글을 조회하고 페이지네이션합니다.</p>
     * <p>배치 조회로 댓글 수와 추천 수 조회</p>
     *
     * @param condition WHERE 조건
     * @param pageable  페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    private Page<PostSearchResult> findPostsWithCondition(BooleanExpression condition, Pageable pageable) {
        // 1. 게시글 기본 정보 조회 (댓글, 추천 JOIN 제외)
        List<PostSearchResult> content = buildBasePostQueryWithoutJoins()
                .where(condition)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(PostSearchResult::getId)
                .toList();

        // 3. 배치로 댓글 수와 추천 수 조회
        Map<Long, Integer> commentCounts = postCommentToPort.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = postLikeQueryPort.findLikeCountsByPostIds(postIds);

        // 4. 댓글 수와 추천 수 설정 - mutable 객체이므로 직접 수정
        content.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getId(), 0));
            post.setLikeCount(likeCounts.getOrDefault(post.getId(), 0));
        });

        // 5. 총 개수 조회
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.user, user)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
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
                        post.content,                      // String content
                        post.views.coalesce(0),           // Integer viewCount
                        Expressions.constant(0),          // Integer likeCount - 나중에 설정
                        post.postCacheFlag,               // PostCacheFlag postCacheFlag
                        post.createdAt,                   // Instant createdAt
                        user.id,                          // Long userId
                        user.userName,                    // String userName
                        Expressions.constant(0),         // Integer commentCount - 나중에 설정
                        post.isNotice))                   // boolean isNotice
                .from(post)
                .leftJoin(post.user, user);
    }

    /**
     * <h3>게시글 상세 정보 JOIN 쿼리</h3>
     * <p>게시글, 좋아요 수, 댓글 수, 사용자 좋아요 여부를 한 번의 JOIN 쿼리로 조회합니다.</p>
     * <p>JOIN으로 필요한 데이터를 한 번에 조회</p>
     * <p>{@link PostQueryService}에서 게시글 상세 페이지 조회 시 호출됩니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @param userId 현재 사용자 ID (좋아요 여부 확인용, null 가능)
     * @return 게시글 상세 정보 프로젝션 (게시글이 없으면 empty)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<PostDetail> findPostDetailWithCounts(Long postId, Long userId) {
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
                        user.id,
                        user.userName,
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
                .leftJoin(post.user, user)
                .leftJoin(postLike).on(postLike.post.id.eq(post.id))
                .leftJoin(comment).on(comment.post.id.eq(post.id))
                .leftJoin(userPostLike).on(
                        userPostLike.post.id.eq(post.id)
                        .and(userId != null ? userPostLike.user.id.eq(userId) : Expressions.FALSE)
                )
                .where(post.id.eq(postId))
                .groupBy(post.id, post.title, post.content, post.views, post.createdAt,
                        user.id, user.userName, post.isNotice, post.postCacheFlag, userPostLike.id)
                .fetchOne();

        return Optional.ofNullable(result);
    }

}