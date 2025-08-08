package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>게시글 레포지터리 공통 기능</h2>
 * <p>
 * 게시글 관련 레포지터리들의 공통 메서드들을 제공하는 추상 클래스
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Repository
@RequiredArgsConstructor
public abstract class PostBaseRepository {

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
     * <h3>게시글 목록 처리 공통 로직</h3>
     * <p>
     * 게시글 튜플 목록을 받아 댓글 수, 추천 수를 조회하고 SimplePostDTO로 변환한다.
     * </p>
     *
     * @param postTuples 조회된 게시글 Tuple 리스트
     * @param post       QPost 엔티티
     * @param user       QUsers 엔티티
     * @param pageable   페이지 정보
     * @return SimplePostDTO 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    protected Page<SimplePostDTO> processPostTuples(List<Tuple> postTuples, QPost post, QUsers user,
            Pageable pageable, Long totalCount) {

        if (postTuples.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<SimplePostDTO> posts = postTuples.stream()
                .map(tuple -> SimplePostDTO.fromTuple(tuple, post, user))
                .collect(Collectors.toList());

        List<Long> postIds = posts.stream()
                .map(SimplePostDTO::getPostId)
                .collect(Collectors.toList());

        Map<Long, Integer> commentCounts = commentRepository.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        posts.forEach(simplePost -> simplePost.withCounts(
                commentCounts.getOrDefault(simplePost.getPostId(), 0),
                likeCounts.getOrDefault(simplePost.getPostId(), 0)
        ));

        return new PageImpl<>(posts, pageable, totalCount != null ? totalCount : 0L);
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
}