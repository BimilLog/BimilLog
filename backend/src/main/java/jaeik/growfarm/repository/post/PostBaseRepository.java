package jaeik.growfarm.repository.post;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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
 * @version 2.0.0
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

        // 좋아요 여부 확인을 위한 서브쿼리
        BooleanExpression userLikedSubquery = (userId != null)
                ? JPAExpressions.selectOne()
                .from(postLike)
                .where(postLike.post.id.eq(post.id).and(postLike.user.id.eq(userId)))
                .exists()
                : Expressions.asBoolean(false);

        // 좋아요 수 계산을 위한 스칼라 서브쿼리
        var likesCountSubquery = JPAExpressions
                .select(postLike.count())
                .from(postLike)
                .where(postLike.post.id.eq(post.id));

        return jpaQueryFactory
                .select(Projections.constructor(FullPostResDTO.class,
                        post.id,
                        user.id,
                        user.userName,
                        post.title,
                        post.content,
                        post.views.coalesce(0),
                        Expressions.numberTemplate(Integer.class, "COALESCE(({0}), 0)", likesCountSubquery), // 스칼라 서브쿼리 사용
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        userLikedSubquery
                ))
                .from(post)
                .leftJoin(post.user, user)
                .where(post.id.eq(postId))
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

        // 댓글 수 계산을 위한 스칼라 서브쿼리
        var commentCountSubquery = JPAExpressions
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(post.id));

        // 좋아요 수 계산을 위한 스칼라 서브쿼리
        var likeCountSubquery = JPAExpressions
                .select(postLike.count())
                .from(postLike)
                .where(postLike.post.id.eq(post.id));

        return jpaQueryFactory
                .select(Projections.constructor(SimplePostResDTO.class,
                        post.id,
                        post.title,
                        post.views.coalesce(0),
                        post.isNotice,
                        post.postCacheFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName,
                        Expressions.numberTemplate(Integer.class, "COALESCE(({0}), 0)", commentCountSubquery), // 스칼라 서브쿼리 사용
                        Expressions.numberTemplate(Integer.class, "COALESCE(({0}), 0)", likeCountSubquery)   // 스칼라 서브쿼리 사용
                ))
                .from(post)
                .leftJoin(post.user, user)
                .where(condition) // isNotice 조건은 호출하는 쪽에서 제어
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * <h3>공지사항 목록 조회</h3>
     * <p>
     * 공지사항으로 설정된 게시글 목록을 최신순으로 조회한다.
     * fetchPosts 메서드를 재사용하여 중복 코드를 제거한다.
     * </p>
     *
     * @return 공지사항 목록
     * @author Jaeik
     * @since 2.0.0
     */
    protected List<SimplePostResDTO> fetchNotices() {
        QPost post = QPost.post;
        // fetchPosts와 동일한 로직이지만 isNotice.isTrue() 조건 사용 -> createPostListQuery 재사용으로 변경
        return createPostListQuery(post.isNotice.isTrue(), Pageable.unpaged());
    }
}