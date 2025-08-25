package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.post.application.port.out.PostQueryPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.post.entity.QPostLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.fulltext.PostFulltextRepository;
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
 * <h2>게시글 쿼리 영속성 어댑터</h2>
 * <p>게시글 조회와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>PostQueryPort 인터페이스를 구현하여 게시글 조회 기능을 제공합니다.</p>
 * <p>단순화된 검색 로직: Strategy Pattern 제거하고 직접 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostQueryAdapter implements PostQueryPort {
    private final JPAQueryFactory jpaQueryFactory;
    private final PostJpaRepository postJpaRepository;
    private final PostFulltextRepository postFullTextRepository;
    private final CommentQueryUseCase commentQueryUseCase;

    private static final QPost post = QPost.post;
    private static final QUser user = QUser.user;
    private static final QPostLike postLike = QPostLike.postLike;

    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id);
    }

    /**
     * <h3>페이지별 게시글 조회</h3>
     * <p>페이지 정보에 따라 게시글 목록을 조회합니다.</p>
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
     * <h3>검색을 통한 게시글 조회</h3>
     * <p>검색 유형과 쿼리에 따라 게시글을 검색하고 페이지네이션합니다.</p>
     * <p>단순화된 검색 로직: 3글자 이상이고 writer가 아니면 FULLTEXT, 아니면 LIKE</p>
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
        if (query == null || query.trim().isEmpty()) {
            return findByPage(pageable);
        }

        String trimmedQuery = query.trim();
        BooleanExpression condition = createSearchCondition(type, trimmedQuery);
        return findPostsWithCondition(condition, pageable);
    }

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
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
     * <p><strong>성능 최적화</strong>: N+1 문제 해결을 위해 댓글 수를 배치 조회로 처리</p>
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
        
        // 1. 게시글 기본 정보 조회 (댓글 JOIN 제외)
        List<PostSearchResult> content = buildBasePostQueryWithoutComments()
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(PostSearchResult::id)
                .toList();

        // 3. 배치로 댓글 수 조회 (N+1 문제 해결)
        Map<Long, Integer> commentCounts = commentQueryUseCase.findCommentCountsByPostIds(postIds);

        // 4. 댓글 수 설정 - PostSearchResult는 immutable이므로 새 객체 생성
        List<PostSearchResult> updatedContent = content.stream()
                .map(post -> PostSearchResult.builder()
                        .id(post.id())
                        .title(post.title())
                        .content(post.content())
                        .viewCount(post.viewCount())
                        .likeCount(post.likeCount())
                        .postCacheFlag(post.postCacheFlag())
                        .createdAt(post.createdAt())
                        .userId(post.userId())
                        .userName(post.userName())
                        .commentCount(commentCounts.getOrDefault(post.id(), 0))
                        .isNotice(post.isNotice())
                        .build())
                .toList();

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .fetchOne();

        return new PageImpl<>(updatedContent, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>단순화된 검색 조건 생성</h3>
     * <p>Strategy Pattern 제거 후 직접 구현한 단순한 검색 로직</p>
     * <p>규칙: 3글자 이상이고 writer가 아니면 FULLTEXT, 아니면 LIKE</p>
     *
     * @param type  검색 유형 (title, writer, title_content)
     * @param query 검색어 (이미 trim된 상태)
     * @return 생성된 BooleanExpression
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createSearchCondition(String type, String query) {
        // 3글자 이상이고 writer가 아니면 FULLTEXT 검색 시도
        boolean shouldTryFullText = query.length() >= 3 && !"writer".equals(type);
        
        if (shouldTryFullText) {
            List<Long> postIds = getPostIdsByFullTextSearch(type, query);
            if (!postIds.isEmpty()) {
                return post.id.in(postIds).and(post.isNotice.isFalse());
            }
            // FULLTEXT 결과가 없으면 LIKE로 fallback
        }
        
        // LIKE 검색
        BooleanExpression likeCondition = switch (type) {
            case "title" -> post.title.containsIgnoreCase(query);
            case "writer" -> createWriterLikeCondition(query);
            case "title_content" -> post.title.containsIgnoreCase(query)
                                   .or(post.content.containsIgnoreCase(query));
            default -> post.title.containsIgnoreCase(query);
        };
        
        return likeCondition.and(post.isNotice.isFalse());
    }

    /**
     * <h3>작성자 LIKE 검색 조건 최적화</h3>
     * <p>글자 수에 따라 검색 패턴을 최적화합니다.</p>
     * <p>1-3글자: %LIKE% (완전 매칭), 4글자+: LIKE% (인덱스 효율성)</p>
     *
     * @param query 검색어
     * @return 최적화된 작성자 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createWriterLikeCondition(String query) {
        if (query.length() >= 4) {
            // 4글자 이상: LIKE% 검색 (인덱스 효율성)
            return user.userName.startsWithIgnoreCase(query);
        } else {
            // 1-3글자: %LIKE% 검색 (완전 일치)
            return user.userName.containsIgnoreCase(query);
        }
    }

    /**
     * <h3>FULLTEXT 검색으로 Post ID 목록 조회</h3>
     * <p>PostFullTextRepository를 사용하여 네이티브 쿼리로 검색합니다.</p>
     * 
     * @param type 검색 유형
     * @param query 검색어
     * @return 검색된 Post ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getPostIdsByFullTextSearch(String type, String query) {
        try {
            String searchTerm = query.trim() + "*";
            
            List<Object[]> results = switch (type) {
                case "title" -> {
                    Pageable fullResults = Pageable.unpaged();
                    yield postFullTextRepository.findByTitleFullText(searchTerm, fullResults);
                }
                case "title_content" -> {
                    Pageable fullResults = Pageable.unpaged();
                    yield postFullTextRepository.findByTitleContentFullText(searchTerm, fullResults);
                }
                default -> List.of();
            };
            
            return results.stream()
                    .map(row -> ((Number) row[0]).longValue())
                    .toList();
                    
        } catch (Exception e) {
            log.warn("FULLTEXT 검색 중 오류 발생 - type: {}, query: {}, LIKE 검색으로 fallback", type, query);
            return List.of(); // 빈 목록 반환하여 LIKE 검색으로 폴백
        }
    }

    /**
     * <h3>공통 게시글 조회 메서드</h3>
     * <p>주어진 조건에 따라 게시글을 조회하고 페이지네이션합니다.</p>
     * <p><strong>성능 최적화</strong>: N+1 문제 해결을 위해 댓글 수를 배치 조회로 처리</p>
     *
     * @param condition WHERE 조건
     * @param pageable  페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    private Page<PostSearchResult> findPostsWithCondition(BooleanExpression condition, Pageable pageable) {
        // 1. 게시글 기본 정보 조회 (댓글 JOIN 제외)
        List<PostSearchResult> content = buildBasePostQueryWithoutComments()
                .where(condition)
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(PostSearchResult::id)
                .toList();

        // 3. 배치로 댓글 수 조회 (N+1 문제 해결)
        Map<Long, Integer> commentCounts = commentQueryUseCase.findCommentCountsByPostIds(postIds);

        // 4. 댓글 수 설정 - PostSearchResult는 immutable이므로 새 객체 생성
        List<PostSearchResult> updatedContent = content.stream()
                .map(post -> PostSearchResult.builder()
                        .id(post.id())
                        .title(post.title())
                        .content(post.content())
                        .viewCount(post.viewCount())
                        .likeCount(post.likeCount())
                        .postCacheFlag(post.postCacheFlag())
                        .createdAt(post.createdAt())
                        .userId(post.userId())
                        .userName(post.userName())
                        .commentCount(commentCounts.getOrDefault(post.id(), 0))
                        .isNotice(post.isNotice())
                        .build())
                .toList();

        // 5. 총 개수 조회
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.user, user)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(updatedContent, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>기본 게시글 쿼리 빌더 (댓글 JOIN 제외)</h3>
     * <p>N+1 문제 해결을 위해 댓글 JOIN을 제외한 쿼리를 생성합니다.</p>
     * <p>댓글 수는 별도의 배치 조회로 처리됩니다.</p>
     *
     * @return JPAQuery<SimplePostResDTO>
     * @author Jaeik
     * @since 2.0.0
     */
    private JPAQuery<PostSearchResult> buildBasePostQueryWithoutComments() {
        return jpaQueryFactory
                .select(Projections.constructor(PostSearchResult.class,
                        post.id,                           // Long id
                        post.title,                        // String title  
                        post.content,                      // String content
                        post.views.coalesce(0),           // Integer viewCount
                        postLike.countDistinct().intValue(), // Integer likeCount
                        post.postCacheFlag,               // PostCacheFlag postCacheFlag
                        post.createdAt,                   // Instant createdAt
                        user.id,                          // Long userId
                        user.userName,                    // String userName
                        Expressions.constant(0),         // Integer commentCount - 나중에 설정
                        post.isNotice))                   // boolean isNotice
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id));
    }
}