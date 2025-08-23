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
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy.SearchStrategyFactory;
import lombok.RequiredArgsConstructor;
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
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostQueryAdapter implements PostQueryPort {
    private final JPAQueryFactory jpaQueryFactory;
    private final PostJpaRepository postJpaRepository;
    private final SearchStrategyFactory searchStrategyFactory;
    private final CommentQueryUseCase commentQueryUseCase;

    private static final QPost post = QPost.post;
    private static final QUser user = QUser.user;
    private static final QPostLike commentLike = QPostLike.postLike;

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
    public Page<SimplePostResDTO> findByPage(Pageable pageable) {
        BooleanExpression condition = post.isNotice.isFalse();
        return findPostsWithCondition(condition, pageable, false);
    }

    /**
     * <h3>검색을 통한 게시글 조회</h3>
     * <p>검색 유형과 쿼리에 따라 게시글을 검색하고 페이지네이션합니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimplePostResDTO> findBySearch(String type, String query, Pageable pageable) {
        BooleanExpression condition = getSearchCondition(type, query);
        return findPostsWithCondition(condition, pageable, true);
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
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        BooleanExpression condition = user.id.eq(userId);
        return findPostsWithCondition(condition, pageable, false);
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
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        QPostLike userPostLike = new QPostLike("userPostLike");
        
        // 1. 게시글 기본 정보 조회 (댓글 JOIN 제외)
        List<SimplePostResDTO> content = buildBasePostQueryWithoutComments()
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(SimplePostResDTO::getId)
                .toList();

        // 3. 배치로 댓글 수 조회 (N+1 문제 해결)
        Map<Long, Integer> commentCounts = commentQueryUseCase.findCommentCountsByPostIds(postIds);

        // 4. 댓글 수 설정
        content.forEach(post -> {
            Integer commentCount = commentCounts.getOrDefault(post.getId(), 0);
            post.setCommentCount(commentCount);
        });

        Long total = jpaQueryFactory
                .select(post.countDistinct())
                .from(post)
                .join(userPostLike).on(post.id.eq(userPostLike.post.id).and(userPostLike.user.id.eq(userId)))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>공통 게시글 조회 메서드</h3>
     * <p>주어진 조건에 따라 게시글을 조회하고 페이지네이션합니다.</p>
     * <p><strong>성능 최적화</strong>: N+1 문제 해결을 위해 댓글 수를 배치 조회로 처리</p>
     *
     * @param condition       WHERE 조건
     * @param pageable       페이지 정보
     * @param isSearchQuery  검색 쿼리 여부 (count 쿼리에서 JOIN 포함 여부 결정)
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    private Page<SimplePostResDTO> findPostsWithCondition(BooleanExpression condition, Pageable pageable, boolean isSearchQuery) {
        // 1. 게시글 기본 정보 조회 (댓글 JOIN 제외)
        List<SimplePostResDTO> content = buildBasePostQueryWithoutComments()
                .where(condition)
                .groupBy(post.id, user.id)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 게시글 ID 목록 추출
        List<Long> postIds = content.stream()
                .map(SimplePostResDTO::getId)
                .toList();

        // 3. 배치로 댓글 수 조회 (N+1 문제 해결)
        Map<Long, Integer> commentCounts = commentQueryUseCase.findCommentCountsByPostIds(postIds);

        // 4. 댓글 수 설정
        content.forEach(post -> {
            Integer commentCount = commentCounts.getOrDefault(post.getId(), 0);
            post.setCommentCount(commentCount);
        });

        Long total = buildCountQuery(condition, isSearchQuery);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
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
    private JPAQuery<SimplePostResDTO> buildBasePostQueryWithoutComments() {
        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,                           // Long id
                        post.title,                        // String title  
                        post.content,                      // String content - 누락되었던 필드 추가
                        post.views.coalesce(0),           // Integer viewCount
                        commentLike.countDistinct().intValue(), // Integer likeCount
                        post.postCacheFlag,               // PostCacheFlag postCacheFlag
                        post.createdAt,                   // Instant createdAt
                        user.id,                          // Long userId
                        user.userName,                    // String userName
                        Expressions.constant(0),         // Integer commentCount - 나중에 설정
                        post.isNotice))                   // boolean isNotice
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(commentLike).on(post.id.eq(commentLike.post.id));
    }

    /**
     * <h3>개수 조회 쿼리 빌더</h3>
     * <p>주어진 조건에 따라 총 개수를 조회합니다.</p>
     *
     * @param condition      WHERE 조건
     * @param isSearchQuery  검색 쿼리 여부 (JOIN 포함 여부 결정)
     * @return 총 개수
     * @author Jaeik
     * @since 2.0.0
     */
    private Long buildCountQuery(BooleanExpression condition, boolean isSearchQuery) {
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(post.count())
                .from(post);

        if (isSearchQuery) {
            countQuery.leftJoin(post.user, user);
        }

        return countQuery.where(condition).fetchOne();
    }

    /**
     * <h3>검색 조건 생성</h3>
     * <p>주어진 검색 유형과 쿼리에 따라 최적화된 검색 조건을 생성합니다.</p>
     * <p>Strategy Pattern과 Factory Pattern을 사용하여 적절한 검색 전략을 선택합니다.</p>
     *
     * @param type  검색 유형
     * @param query 검색어
     * @return 생성된 BooleanExpression
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression getSearchCondition(String type, String query) {
        return searchStrategyFactory.createSearchCondition(type, query);
    }
}
