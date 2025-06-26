package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>게시글 레포지터리 공통 기능</h2>
 * <p>
 * 게시글 관련 레포지터리들의 공통 메서드들을 제공하는 추상 클래스
 * </p>
 *
 * @author Jaeik
 * @version 1.0
 */
@Repository
@RequiredArgsConstructor
public abstract class PostCustomBaseRepository {

    protected final JPAQueryFactory jpaQueryFactory;
    protected final CommentRepository commentRepository;

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
    protected List<Tuple> fetchPosts(QPost post, QUsers user, BooleanExpression condition, Pageable pageable) {
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
                .leftJoin(post.user, user)
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
    protected List<Long> extractPostIds(List<Tuple> postTuples, QPost post) {
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
    protected Map<Long, Integer> fetchLikeCounts(List<Long> postIds) {
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
    protected List<SimplePostDTO> createSimplePostDTOs(List<Tuple> postTuples, QPost post, QUsers user,
            Map<Long, Integer> commentCounts, Map<Long, Integer> likeCounts, boolean userLike) {
        return postTuples.stream()
                .map(tuple -> {
                    Integer views = tuple.get(post.views.coalesce(0));
                    Long userId = tuple.get(post.user.id);
                    String userName = tuple.get(user.userName);
                    Boolean isNotice = tuple.get(post.isNotice);

                    SimplePostDTO dto = new SimplePostDTO(
                            tuple.get(post.id),
                            userId,
                            userName != null ? userName : "익명",
                            tuple.get(post.title),
                            commentCounts.getOrDefault(tuple.get(post.id), 0),
                            likeCounts.getOrDefault(tuple.get(post.id), 0),
                            views != null ? views : 0,
                            tuple.get(post.createdAt),
                            Boolean.TRUE.equals(isNotice));

                    // PopularFlag 설정
                    dto.setPopularFlag(tuple.get(post.popularFlag));

                    return dto;
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
    protected Page<SimplePostDTO> processPostTuples(List<Tuple> postTuples, QPost post, QUsers user,
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
    protected Long fetchTotalCount(QPost post, QUsers user, BooleanExpression condition) {
        JPAQuery<Long> query = jpaQueryFactory
                .select(post.count())
                .from(post);

        if (condition != null && condition.toString().contains("users")) {
            query = query.leftJoin(post.user, user);
        }

        return query.where(condition).fetchOne();
    }

    /**
     * <h3>작성자 검색 조건 생성</h3>
     * <p>
     * 작성자 검색을 위한 조건을 생성한다.
     * </p>
     *
     * @param user    QUsers 엔티티
     * @param keyword 검색어
     * @return BooleanExpression 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    protected BooleanExpression createAuthorSearchCondition(QUsers user, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.length() >= 4) {
            return user.userName.startsWithIgnoreCase(trimmedKeyword);
        } else {
            return user.userName.containsIgnoreCase(trimmedKeyword);
        }
    }

    /**
     * <h3>네이티브 쿼리 결과를 SimplePostDTO로 변환</h3>
     *
     * @param results 네이티브 쿼리 결과
     * @return SimplePostDTO 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    protected List<SimplePostDTO> convertNativeQueryResults(List<Object[]> results) {
        return results.stream()
                .map(row -> {
                    // BigInteger 안전 변환
                    Long postId = convertToLong(row[0]);
                    String title = (String) row[1];
                    int views = row[2] != null ? (Integer) row[2] : 0;
                    Boolean isNotice = (Boolean) row[3];
                    PopularFlag popularFlag = row[4] != null ? PopularFlag.valueOf((String) row[4])
                            : null;
                    java.sql.Timestamp createdAt = (java.sql.Timestamp) row[5];
                    Long userId = row[6] != null ? convertToLong(row[6]) : null;
                    String userName = row[7] != null ? (String) row[7] : "익명";

                    SimplePostDTO dto = new SimplePostDTO(
                            postId,
                            userId,
                            userName,
                            title,
                            0,
                            0,
                            views,
                            createdAt.toInstant(),
                            Boolean.TRUE.equals(isNotice));
                    dto.setPopularFlag(popularFlag);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * <h3>Object를 Long으로 안전하게 변환</h3>
     * <p>
     * BigInteger, Long, Integer 등을 Long 타입으로 안전하게 변환한다.
     * </p>
     *
     * @param obj 변환할 객체
     * @return Long 값
     * @author Jaeik
     * @since 1.0.0
     */
    private Long convertToLong(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof BigInteger) {
            return ((BigInteger) obj).longValue();
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        throw new IllegalArgumentException("Unsupported type for Long conversion: " + obj.getClass());
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
    protected List<SimplePostDTO> convertTuplesToSimplePostDTOs(List<Tuple> tuples, QPost post, QUsers user,
            QComment comment, QPostLike postLike) {
        return tuples.stream()
                .map(tuple -> new SimplePostDTO(
                        tuple.get(post.id),
                        tuple.get(post.user.id),
                        tuple.get(user.userName),
                        tuple.get(post.title),
                        tuple.get(comment.count()) != null ? Objects
                                .requireNonNull(tuple.get(comment.count())).intValue()
                                : 0,
                        tuple.get(postLike.count()) != null ? Objects
                                .requireNonNull(tuple.get(postLike.count())).intValue()
                                : 0,
                        tuple.get(post.views) != null ? tuple.get(post.views) : 0,
                        tuple.get(post.createdAt),
                        tuple.get(post.isNotice) != null
                                && Boolean.TRUE.equals(tuple.get(post.isNotice)),
                        tuple.get(user)))
                .collect(Collectors.toList());
    }
}