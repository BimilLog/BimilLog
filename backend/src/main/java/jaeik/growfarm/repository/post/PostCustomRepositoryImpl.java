package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentCustomRepository;
import jaeik.growfarm.util.FullTextSearchUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>PostCustomRepositoryImpl</h2>
 * <p>
 * 게시글 관련 커스텀 쿼리 메소드 구현체
 * </p>
 *
 * @author jaeik
 * @version 1.0
 */
@Repository
public class PostCustomRepositoryImpl implements PostCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final CommentCustomRepository commentCustomRepository;

    public PostCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory, CommentCustomRepository commentCustomRepository) {
        this.jpaQueryFactory = jpaQueryFactory;
        this.commentCustomRepository = commentCustomRepository;
    }

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시글 목록을 조회한다.
     * </p>
     * <p>
     * 게시글 당 댓글 수, 추천 수를 반환한다.
     * </p>
     *
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable) {
        return findPostsInternal(null, null, pageable);
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색한다.
     * </p>
     * <p>
     * 게시글 마다의 총 댓글 수, 총 추천 수를 반환한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable) {
        return findPostsInternal(keyword, searchType, pageable);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null 가능)
     * @return PostDTO 게시글 상세 정보
     * @since 1.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public PostDTO findPostById(Long postId, Long userId) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        Tuple postTuple = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.content,
                        post.views,
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .innerJoin(post.user, user)
                .where(post.id.eq(postId))
                .fetchOne();

        if (postTuple == null) {
            return null;
        }

        List<Long> postIds = Collections.singletonList(postId);

        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        boolean userLike = false;
        if (userId != null) {
            QPostLike userPostLike = QPostLike.postLike;
            Long likeCount = jpaQueryFactory
                    .select(userPostLike.count())
                    .from(userPostLike)
                    .where(userPostLike.post.id.eq(postId).and(userPostLike.user.id.eq(userId)))
                    .fetchOne();
            userLike = likeCount != null && likeCount > 0;
        }

        Integer views = postTuple.get(post.views);
        return PostDTO.existedPost(
                postTuple.get(post.id),
                postTuple.get(post.user.id),
                postTuple.get(user.userName),
                postTuple.get(post.title),
                postTuple.get(post.content),
                views != null ? views : 0,
                likeCounts.getOrDefault(postId, 0),
                Boolean.TRUE.equals(postTuple.get(post.isNotice)),
                postTuple.get(post.popularFlag),
                postTuple.get(post.createdAt),
                userLike);
    }

    /**
     * <h3>게시글 조회 공통 로직</h3>
     * <p>
     * 게시글 목록 조회와 검색 기능의 공통 로직을 처리한다.
     * </p>
     *
     * @param keyword    검색어 (null 또는 빈 문자열이면 전체 조회)
     * @param searchType 검색 유형 (null이면 전체 조회)
     * @param pageable   페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    private Page<SimplePostDTO> findPostsInternal(String keyword, String searchType, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        BooleanExpression searchCondition = createSearchCondition(post, user, keyword, searchType);

        BooleanExpression baseCondition = post.isNotice.eq(false);
        BooleanExpression finalCondition = searchCondition != null
                ? baseCondition.and(searchCondition)
                : baseCondition;

        List<Tuple> postTuples = fetchPosts(post, user, finalCondition, pageable);

        if (postTuples.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> postIds = extractPostIds(postTuples, post);

        Map<Long, Integer> commentCounts = commentCustomRepository.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        List<SimplePostDTO> results = createSimplePostDTOs(postTuples, post, user, commentCounts, likeCounts);

        Long total = fetchTotalCount(post, user, finalCondition);

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    /**
     * <h3>게시글 조회</h3>
     * <p>
     * 게시글 목록을 조회한다.
     * </p>
     *
     * @param post      QPost 엔티티
     * @param user      QUsers 엔티티
     * @param condition 검색 조건
     * @param pageable  페이지 정보
     * @return 조회된 게시글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    private List<Tuple> fetchPosts(QPost post, QUsers user, BooleanExpression condition, Pageable pageable) {
        return jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .join(post.user, user)
                .where(condition)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * <h3>게시글 ID 추출</h3>
     * <p>
     * Tuple에서 게시글 ID를 추출한다.
     * </p>
     *
     * @param postTuples 조회된 게시글 Tuple 리스트
     * @param post       QPost 엔티티
     * @return 게시글 ID 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    private List<Long> extractPostIds(List<Tuple> postTuples, QPost post) {
        return postTuples.stream()
                .map(tuple -> tuple.get(post.id))
                .collect(Collectors.toList());
    }

    /**
     * <h3>추천 수 조회</h3>
     * <p>
     * 게시글별 추천 수를 조회한다.
     * </p>
     *
     * @param postIds 게시글 ID 리스트
     * @return 게시글 ID와 추천 수의 맵
     * @author Jaeik
     * @since 1.0.0
     */
    private Map<Long, Integer> fetchLikeCounts(List<Long> postIds) {
        QPostLike postLike = QPostLike.postLike;
        NumberExpression<Long> likeCountExpr = postLike.count().coalesce(0L).as("likeCount");

        return jpaQueryFactory
                .select(postLike.post.id, likeCountExpr)
                .from(postLike)
                .where(postLike.post.id.in(postIds))
                .groupBy(postLike.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(postLike.post.id),
                        tuple -> {
                            Long count = tuple.get(likeCountExpr);
                            return count != null ? Math.toIntExact(count) : 0;
                        }));
    }

    /**
     * <h3>SimplePostDTO 생성</h3>
     * <p>
     * Tuple에서 SimplePostDTO 리스트를 생성한다.
     * </p>
     *
     * @param postTuples    조회된 게시글 Tuple 리스트
     * @param post          QPost 엔티티
     * @param user          QUsers 엔티티
     * @param commentCounts 댓글 수 맵
     * @param likeCounts    추천 수 맵
     * @return SimplePostDTO 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    private List<SimplePostDTO> createSimplePostDTOs(List<Tuple> postTuples, QPost post, QUsers user,
            Map<Long, Integer> commentCounts, Map<Long, Integer> likeCounts) {
        return postTuples.stream()
                .map(tuple -> {
                    Integer views = tuple.get(post.views.coalesce(0));
                    return new SimplePostDTO(
                            tuple.get(post.id),
                            tuple.get(post.user.id),
                            tuple.get(user.userName),
                            tuple.get(post.title),
                            commentCounts.getOrDefault(tuple.get(post.id), 0),
                            likeCounts.getOrDefault(tuple.get(post.id), 0),
                            views != null ? views : 0,
                            tuple.get(post.createdAt),
                            false);
                })
                .collect(Collectors.toList());
    }

    /**
     * <h3>전체 게시글 수 조회</h3>
     * <p>
     * 검색 조건에 맞는 전체 게시글 수를 조회한다.
     * </p>
     *
     * @param post      QPost 엔티티
     * @param user      QUsers 엔티티
     * @param condition 검색 조건
     * @return 전체 게시글 수
     * @author Jaeik
     * @since 1.0.0
     */
    private Long fetchTotalCount(QPost post, QUsers user, BooleanExpression condition) {
        JPAQuery<Long> query = jpaQueryFactory
                .select(post.count())
                .from(post);

        // 검색 조건이 있는 경우 user 조인 필요
        if (condition.toString().contains("users")) {
            query = query.join(post.user, user);
        }

        return query.where(condition).fetchOne();
    }

    /**
     * <h3>검색 조건 생성</h3>
     * <p>
     * 검색어와 검색 유형에 따라 검색 조건을 생성한다.
     * </p>
     *
     * @param post       QPost 엔티티
     * @param user       QUsers 엔티티
     * @param keyword    검색어
     * @param searchType 검색 유형
     * @return BooleanExpression 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    private BooleanExpression createSearchCondition(QPost post, QUsers user, String keyword, String searchType) {
        if (keyword == null || keyword.trim().isEmpty() || searchType == null) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        return switch (searchType) {
            case "TITLE" -> FullTextSearchUtils.matchTitle(post.title, trimmedKeyword);
            case "TITLE_CONTENT" -> FullTextSearchUtils.matchAgainst(post.title, post.content, trimmedKeyword);
            case "AUTHOR" -> FullTextSearchUtils.optimizedUsernameSearch(user.userName, trimmedKeyword);
            default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
        };
    }

    // 1일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 실시간 인기글로 등록
    @Override
    public void updateRealtimePopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 1. 모든 게시글의 실시간 인기글 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.popularFlag, (PopularFlag) null)
                .execute();

        // 2. 실시간 인기글 조건에 맞는 상위 5개 조회
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(1, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        // 3. 해당 게시글들의 실시간 인기글 컬럼 REALTIME으로 설정
        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.popularFlag, PopularFlag.REALTIME)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }
    }

    // 7일 이내의 글 중에서 추천 수가 가장 높은 글 상위 5개를 주간 인기글로 등록
    @Override
    public List<Post> updateWeeklyPopularPosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 기존 주간 인기글 초기화
        jpaQueryFactory.update(post)
                .set(post.popularFlag, (PopularFlag) null)
                .execute();

        // 7일 이내의 글 중 추천 수 많은 상위 5개 선정
        List<Long> popularPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .where(post.createdAt.after(Instant.now().minus(7, ChronoUnit.DAYS)))
                .groupBy(post.id)
                .orderBy(postLike.count().desc())
                .limit(5)
                .fetch();

        if (!popularPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.popularFlag, PopularFlag.WEEKLY)
                    .where(post.id.in(popularPostIds))
                    .execute();
        }

        if (popularPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(popularPostIds))
                .fetch();
    }

    // 추천 수가 20개 이상인 글을 명예의 전당에 등록
    @Override
    public List<Post> updateHallOfFamePosts() {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 추천 수 20개 이상인 게시글 ID 추출
        List<Long> hofPostIds = jpaQueryFactory
                .select(post.id)
                .from(post)
                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                .groupBy(post.id)
                .having(postLike.count().goe(20))
                .fetch();

        // 먼저 기존 컬럼 초기화
        jpaQueryFactory.update(post)
                .set(post.popularFlag, (PopularFlag) null)
                .execute();

        if (!hofPostIds.isEmpty()) {
            jpaQueryFactory.update(post)
                    .set(post.popularFlag, PopularFlag.LEGEND)
                    .where(post.id.in(hofPostIds))
                    .execute();
        }

        if (hofPostIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(post)
                .where(post.id.in(hofPostIds))
                .fetch();
    }

    // 해당 유저가 추천 누른 글 목록 반환
    @Override
    public Page<Post> findByLikedPosts(Long userId, Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        // 사용자가 좋아요한 게시물 ID 목록 조회 (페이징 적용)
        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .orderBy(post.createdAt.desc()) // 최신순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 실행
        Long total = jpaQueryFactory
                .select(post.count())
                .from(post)
                .join(postLike)
                .on(post.id.eq(postLike.post.id))
                .where(postLike.user.id.eq(userId))
                .fetchOne();

        // null 안전성을 고려한 코드로 수정
        return new PageImpl<>(posts, pageable, total == null ? 0L : total);
    }
}
