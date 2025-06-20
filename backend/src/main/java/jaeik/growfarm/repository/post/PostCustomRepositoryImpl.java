package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.util.FullTextSearchUtils;
import lombok.RequiredArgsConstructor;
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
import java.util.Objects;
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
@RequiredArgsConstructor
public class PostCustomRepositoryImpl implements PostCustomRepository {

        private final JPAQueryFactory jpaQueryFactory;
        private final CommentRepository commentRepository;



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
         * @author Jaeik
         * @since 1.0.0
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
                Long total = fetchTotalCount(post, user, finalCondition);

                return processPostTuples(postTuples, post, user, pageable, false, total);
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
         * @param userLike      사용자 좋아요 여부
         * @return SimplePostDTO 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        private List<SimplePostDTO> createSimplePostDTOs(List<Tuple> postTuples, QPost post, QUsers user,
                        Map<Long, Integer> commentCounts, Map<Long, Integer> likeCounts, boolean userLike) {
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
                                                        userLike);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * <h3>게시글 목록 처리 공통 로직</h3>
         * <p>
         * 게시글 튜플 목록을 받아 댓글 수, 추천 수를 조회하고 SimplePostDTO로 변환한다.
         * </p>
         *
         * @param postTuples 조회된 게시글 Tuple 리스트
         * @param post       QPost 엔티티
         * @param user       QUsers 엔티티
         * @param pageable   페이지 정보
         * @param userLike   사용자 좋아요 여부
         * @return SimplePostDTO 페이지
         * @author Jaeik
         * @since 1.0.0
         */
        private Page<SimplePostDTO> processPostTuples(List<Tuple> postTuples, QPost post, QUsers user,
                        Pageable pageable, boolean userLike, Long totalCount) {
                if (postTuples.isEmpty()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, 0);
                }

                List<Long> postIds = extractPostIds(postTuples, post);

                Map<Long, Integer> commentCounts = commentRepository.findCommentCountsByPostIds(postIds);
                Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

                List<SimplePostDTO> results = createSimplePostDTOs(postTuples, post, user, commentCounts, likeCounts,
                                userLike);

                return new PageImpl<>(results, pageable, totalCount != null ? totalCount : 0L);
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
                        case "TITLE_CONTENT" ->
                                FullTextSearchUtils.matchAgainst(post.title, post.content, trimmedKeyword);
                        case "AUTHOR" -> FullTextSearchUtils.optimizedUsernameSearch(user.userName, trimmedKeyword);
                        default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
                };
        }

        /**
         * <h3>실시간 인기글 선정</h3>
         * <p>
         * 1일 이내의 글 중 추천 수가 가장 높은 상위 5개를 실시간 인기글로 등록한다.
         * </p>
         *
         * @return 실시간 인기글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateRealtimePopularPosts() {
                return updatePopularPosts(1, PopularFlag.REALTIME);
        }

        /**
         * <h3>주간 인기글 선정</h3>
         * <p>
         * 7일 이내의 글 중 추천 수가 가장 높은 상위 5개를 주간 인기글로 등록한다.
         * </p>
         *
         * @return 주간 인기글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateWeeklyPopularPosts() {
                return updatePopularPosts(7, PopularFlag.WEEKLY);
        }

        /**
         * <h3>인기글 선정 공통 로직</h3>
         * <p>
         * 지정된 기간 이내의 글 중 추천 수가 가장 높은 상위 5개를 인기글로 등록한다.
         * </p>
         *
         * @param days        조회 기간 (일 단위)
         * @param popularFlag 설정할 인기글 플래그
         * @return 인기글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        private List<SimplePostDTO> updatePopularPosts(int days, PopularFlag popularFlag) {
                QPost post = QPost.post;
                QPostLike postLike = QPostLike.postLike;
                QComment comment = QComment.comment;
                QUsers user = QUsers.users;

                jpaQueryFactory.update(post)
                                .set(post.popularFlag, (PopularFlag) null)
                                .where(post.popularFlag.eq(popularFlag))
                                .execute();

                List<Tuple> popularPostsData = jpaQueryFactory
                                .select(
                                                post.id,
                                                post.user.id,
                                                user.userName,
                                                post.title,
                                                post.views,
                                                post.createdAt,
                                                post.isNotice,
                                                postLike.count().coalesce(0L),
                                                comment.count().coalesce(0L),
                                                user)
                                .from(post)
                                .leftJoin(user).on(post.user.id.eq(user.id))
                                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                                .leftJoin(comment).on(post.id.eq(comment.post.id))
                                .where(post.createdAt.after(Instant.now().minus(days, ChronoUnit.DAYS)))
                                .groupBy(
                                                post.id,
                                                post.user.id,
                                                user.userName,
                                                post.title,
                                                post.views,
                                                post.createdAt,
                                                post.isNotice,
                                                user)
                                .orderBy(postLike.count().desc())
                                .limit(5)
                                .fetch();

                if (popularPostsData.isEmpty()) {
                        return Collections.emptyList();
                }

                List<Long> popularPostIds = popularPostsData.stream()
                                .map(tuple -> tuple.get(post.id))
                                .collect(Collectors.toList());

                jpaQueryFactory.update(post)
                                .set(post.popularFlag, popularFlag)
                                .where(post.id.in(popularPostIds))
                                .execute();

                return convertTuplesToSimplePostDTOs(popularPostsData, post, user, comment, postLike);
        }

        /**
         * <h3>레전드 게시글 선정</h3>
         * <p>
         * 추천 수가 20개 이상인 모든 게시글을 레전드 게시글로 등록한다.
         * </p>
         *
         * @return 레전드 게시글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional
        public List<SimplePostDTO> updateLegendPosts() {
                QPost post = QPost.post;
                QPostLike postLike = QPostLike.postLike;
                QComment comment = QComment.comment;
                QUsers user = QUsers.users;

                jpaQueryFactory.update(post)
                                .set(post.popularFlag, (PopularFlag) null)
                                .where(post.popularFlag.eq(PopularFlag.LEGEND))
                                .execute();

                List<Tuple> legendPostsData = jpaQueryFactory
                                .select(
                                                post.id,
                                                post.user.id,
                                                user.userName,
                                                post.title,
                                                post.views,
                                                post.createdAt,
                                                post.isNotice,
                                                postLike.count().coalesce(0L),
                                                comment.count().coalesce(0L),
                                                user)
                                .from(post)
                                .leftJoin(user).on(post.user.id.eq(user.id))
                                .leftJoin(postLike).on(post.id.eq(postLike.post.id))
                                .leftJoin(comment).on(post.id.eq(comment.post.id))
                                .groupBy(
                                                post.id,
                                                post.user.id,
                                                user.userName,
                                                post.title,
                                                post.views,
                                                post.createdAt,
                                                post.isNotice,
                                                user)
                                .having(postLike.count().goe(20))
                                .orderBy(postLike.count().desc())
                                .fetch();

                if (legendPostsData.isEmpty()) {
                        return Collections.emptyList();
                }

                List<Long> legendPostIds = legendPostsData.stream()
                                .map(tuple -> tuple.get(post.id))
                                .collect(Collectors.toList());

                jpaQueryFactory.update(post)
                                .set(post.popularFlag, PopularFlag.LEGEND)
                                .where(post.id.in(legendPostIds))
                                .execute();

                return convertTuplesToSimplePostDTOs(legendPostsData, post, user, comment, postLike);
        }

        /**
         * <h3>Tuple을 SimplePostDTO로 변환</h3>
         * <p>
         * 조회된 Tuple 데이터를 SimplePostDTO 리스트로 변환하는 공통 메서드
         * </p>
         *
         * @param tuples   조회된 Tuple 리스트
         * @param post     QPost 엔티티
         * @param user     QUsers 엔티티
         * @param comment  QComment 엔티티
         * @param postLike QPostLike 엔티티
         * @return SimplePostDTO 리스트
         * @author Jaeik
         * @since 1.0.0
         */
        private List<SimplePostDTO> convertTuplesToSimplePostDTOs(List<Tuple> tuples, QPost post, QUsers user,
                        QComment comment, QPostLike postLike) {
                return tuples.stream()
                                .map(tuple -> new SimplePostDTO(
                                                tuple.get(post.id),
                                                tuple.get(post.user.id),
                                                tuple.get(user.userName),
                                                tuple.get(post.title),
                                                Objects.requireNonNull(tuple.get(comment.count())).intValue(),
                                                Objects.requireNonNull(tuple.get(postLike.count())).intValue(),
                                                tuple.get(post.views) != null
                                                                ? Objects.requireNonNull(tuple.get(post.views))
                                                                : 0,
                                                tuple.get(post.createdAt),
                                                tuple.get(post.isNotice) != null
                                                                && Boolean.TRUE.equals(tuple.get(post.isNotice)),
                                                tuple.get(user) // Users 엔티티 전달
                                ))
                                .collect(Collectors.toList());
        }

        /**
         * <h3>사용자 작성 글 목록 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 작성한 글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이지 정보
         * @return 사용자가 작성한 글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> findPostsByUserId(Long userId, Pageable pageable) {
                QPost post = QPost.post;
                QUsers user = QUsers.users;

                // 해당 사용자가 작성한 글만 조회하는 조건
                BooleanExpression userCondition = post.user.id.eq(userId);
                BooleanExpression baseCondition = post.isNotice.eq(false);
                BooleanExpression finalCondition = baseCondition.and(userCondition);

                List<Tuple> postTuples = fetchPosts(post, user, finalCondition, pageable);
                Long total = fetchTotalCount(post, user, finalCondition);

                return processPostTuples(postTuples, post, user, pageable, false, total);
        }

        /**
         * <h3>사용자가 추천한 글 목록 조회</h3>
         * <p>
         * 사용자 ID를 기준으로 해당 사용자가 추천한 글 목록을 조회한다.
         * </p>
         *
         * @param userId   사용자 ID
         * @param pageable 페이지 정보
         * @return 사용자가 추천한 글 목록
         * @author Jaeik
         * @since 1.0.0
         */
        @Override
        @Transactional(readOnly = true)
        public Page<SimplePostDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
                QPost post = QPost.post;
                QUsers user = QUsers.users;
                QPostLike postLike = QPostLike.postLike;

                // 사용자가 추천한 게시글 조회
                List<Tuple> postTuples = jpaQueryFactory
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
                                .join(postLike).on(postLike.post.id.eq(post.id))
                                .where(postLike.user.id.eq(userId))
                                .orderBy(post.createdAt.desc()) // 게시글 생성일 기준 최신순 정렬
                                .offset(pageable.getOffset())
                                .limit(pageable.getPageSize())
                                .fetch();

                Long total = jpaQueryFactory
                                .select(post.count())
                                .from(post)
                                .join(postLike).on(postLike.post.id.eq(post.id))
                                .where(postLike.user.id.eq(userId))
                                .fetchOne();

                return processPostTuples(postTuples, post, user, pageable, true, total);
        }
}
