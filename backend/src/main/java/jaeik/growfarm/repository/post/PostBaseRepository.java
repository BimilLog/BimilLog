package jaeik.growfarm.repository.post;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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

     * @param condition 검색 조건
     * @param pageable  페이지 정보
     * @return 조회된 게시글 목록
     * @author Jaeik
     * @since 1.0.0
     */
    protected Page<SimplePostResDTO> fetchPosts(BooleanExpression condition, Pageable pageable) {
        List<SimplePostResDTO> posts = createPostListQuery(condition, pageable);
        Long totalCount = createPostCountQuery(condition);
        return new PageImpl<>(posts, pageable, totalCount);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 상세 정보를 조회한다. 사용자 ID가 주어지면 좋아요 여부도 함께 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null일 경우 좋아요 여부는 조회하지 않음)
     * @return 게시글 상세 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    protected FullPostResDTO fetchPostDetail(Long postId, Long userId) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
        QPostLike postLike = QPostLike.postLike;

        BooleanExpression userLikeCondition = (userId != null)
                ? postLike.user.id.eq(userId)
                : Expressions.asBoolean(false);

        return jpaQueryFactory
                .select(Projections.constructor(FullPostResDTO.class,
                        post.id,
                        user.id,
                        user.userName,
                        post.title,
                        post.content,
                        post.views.coalesce(0),
                        postLike.count().intValue(),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        new CaseBuilder()
                                .when(userLikeCondition)
                                .then(true)
                                .otherwise(false)
                ))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(postLike).on(postLike.post.id.eq(post.id))
                .where(post.id.eq(postId))
                .groupBy(post.id, user.id, user.userName, post.title, post.content,
                        post.views, post.isNotice, post.postCacheFlag, post.createdAt)
                .fetchOne();
    }


    /**
     * <h3>전체 게시글 수 조회</h3>
     * <p>
     * 검색 조건에 맞는 전체 게시글 수를 조회한다.
     * </p>
     *
     * @param condition 검색 조건
     * @return 전체 게시글 수
     * @author Jaeik
     * @since 2.0.0
     */
    protected Long createPostCountQuery(BooleanExpression condition) {
        QPost post = QPost.post;

        return jpaQueryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.user, QUsers.users)
                .where(post.isNotice.isFalse().and(condition)) // 기본 조건을 명시적으로 추가
                .fetchOne();
    }

    /**
     * <h3>게시글 목록 조회 공통 쿼리</h3>
     * <p>
     * 동적 검색 조건과 페이징 정보를 받아 게시글 목록을 조회한다.
     * </p>
     *
     * @param condition 검색 조건
     * @param pageable  페이지 정보
     * @return 조회된 게시글 목록
     */
    protected List<SimplePostResDTO> createPostListQuery(BooleanExpression condition, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;

        BooleanExpression finalCondition = post.isNotice.isFalse().and(condition); // 기본 조건을 명시적으로 추가

        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id, post.title, post.views.coalesce(0), post.isNotice, post.postCacheFlag,
                        post.createdAt, post.user.id, user.userName,
                        comment.countDistinct().intValue(), postLike.countDistinct().intValue()))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(comment).on(comment.post.id.eq(post.id))
                .leftJoin(postLike).on(postLike.post.id.eq(post.id))
                .where(finalCondition)
                .groupBy(post.id, post.title, post.views, post.isNotice, post.postCacheFlag,
                        post.createdAt, user.id, user.userName)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }


}