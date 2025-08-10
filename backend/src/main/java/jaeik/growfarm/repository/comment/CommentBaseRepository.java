package jaeik.growfarm.repository.comment;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.entity.comment.QCommentClosure;
import jaeik.growfarm.entity.comment.QCommentLike;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.user.QUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h2>댓글 레포지터리 공통 기능</h2>
 * <p>
 * 댓글 관련 레포지터리들의 공통 메서드들을 제공하는 추상 클래스
 * Post BaseRepository 구조를 참조하여 SOLID 원칙 적용
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public abstract class CommentBaseRepository {

    protected final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>삭제된 메시지 상수</h3>
     * <p>댓글이 논리 삭제되었을 때 표시되는 메시지</p>
     */
    protected static final String DELETED_MESSAGE = "삭제된 메시지입니다.";

    /**
     * <h3>인기 댓글 최소 추천 수</h3>
     * <p>인기 댓글로 분류되기 위한 최소 추천 수</p>
     */
    protected static final long POPULAR_COMMENT_MIN_LIKES = 3L;

    /**
     * <h3>인기 댓글 최대 개수</h3>
     * <p>조회할 인기 댓글의 최대 개수</p>
     */
    protected static final int POPULAR_COMMENT_LIMIT = 3;

    /**
     * <h3>일반 댓글 페이지 크기</h3>
     * <p>일반 댓글 조회 시 기본 페이지 크기</p>
     */
    protected static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * <h3>게시글별 댓글 수 조회 공통 로직</h3>
     * <p>게시글 ID 리스트로 각 게시글의 댓글 수를 조회하는 공통 메서드</p>
     *
     * @param postIds 게시글 ID 리스트
     * @return 게시글 ID와 댓글 수의 맵
     * @author Jaeik
     * @since 2.0.0
     */
    protected Map<Long, Integer> getCommentCountsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        QComment comment = QComment.comment;

        return jpaQueryFactory
                .select(comment.post.id, comment.count().intValue())
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        tuple -> tuple.get(comment.post.id),
                        tuple -> tuple.get(1, Integer.class),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * <h3>삭제되지 않은 댓글 조건</h3>
     * <p>논리 삭제되지 않은 댓글만 조회하기 위한 조건</p>
     *
     * @return BooleanExpression 조건
     * @author Jaeik
     * @since 2.0.0
     */
    protected BooleanExpression notDeletedCondition() {
        QComment comment = QComment.comment;
        return comment.content.ne(DELETED_MESSAGE);
    }

    /**
     * <h3>특정 게시글 댓글 조건</h3>
     * <p>특정 게시글에 속한 댓글만 조회하기 위한 조건</p>
     *
     * @param postId 게시글 ID
     * @return BooleanExpression 조건
     * @author Jaeik
     * @since 2.0.0
     */
    protected BooleanExpression postIdCondition(Long postId) {
        QComment comment = QComment.comment;
        return comment.post.id.eq(postId);
    }

    /**
     * <h3>특정 사용자 댓글 조건</h3>
     * <p>특정 사용자가 작성한 댓글만 조회하기 위한 조건</p>
     *
     * @param userId 사용자 ID
     * @return BooleanExpression 조건
     * @author Jaeik
     * @since 2.0.0
     */
    protected BooleanExpression userIdCondition(Long userId) {
        QComment comment = QComment.comment;
        return comment.user.id.eq(userId);
    }

    /**
     * <h3>댓글 좋아요 수 계산</h3>
     * <p>댓글의 좋아요 수를 계산하는 공통 표현식</p>
     *
     * @return NumberExpression<Long> 좋아요 수 표현식
     * @author Jaeik
     * @since 2.0.0
     */
    protected NumberExpression<Long> getLikeCountExpression() {
        QCommentLike commentLike = QCommentLike.commentLike;
        return commentLike.count();
    }

    /**
     * <h3>루트 댓글 조건</h3>
     * <p>루트 댓글(depth = 0)만 조회하기 위한 조건</p>
     *
     * @return BooleanExpression 조건
     * @author Jaeik
     * @since 2.0.0
     */
    protected BooleanExpression rootCommentCondition() {
        QCommentClosure closure = QCommentClosure.commentClosure;
        return closure.depth.eq(0);
    }

    /**
     * <h3>댓글 존재 여부 확인</h3>
     * <p>특정 댓글이 존재하는지 확인</p>
     *
     * @param commentId 댓글 ID
     * @return boolean 존재 여부
     * @author Jaeik
     * @since 2.0.0
     */
    protected boolean existsCommentById(Long commentId) {
        if (commentId == null) {
            return false;
        }
        
        QComment comment = QComment.comment;
        return jpaQueryFactory
                .select(comment.id)
                .from(comment)
                .where(comment.id.eq(commentId))
                .fetchFirst() != null;
    }

    /**
     * <h3>사용자별 댓글 수 조회</h3>
     * <p>특정 사용자가 작성한 전체 댓글 수를 조회</p>
     *
     * @param userId 사용자 ID
     * @param includeDeleted 삭제된 댓글 포함 여부
     * @return Long 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    protected Long getCommentCountByUserId(Long userId, boolean includeDeleted) {
        QComment comment = QComment.comment;
        
        BooleanExpression condition = userIdCondition(userId);
        if (!includeDeleted) {
            condition = condition.and(notDeletedCondition());
        }
        
        return jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(condition)
                .fetchOne();
    }

    /**
     * <h3>게시글별 댓글 수 조회 (단일)</h3>
     * <p>특정 게시글의 댓글 수를 조회</p>
     *
     * @param postId 게시글 ID
     * @return Long 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    protected Long getCommentCountByPostId(Long postId) {
        QComment comment = QComment.comment;
        
        return jpaQueryFactory
                .select(comment.count())
                .from(comment)
                .where(postIdCondition(postId))
                .fetchOne();
    }

    /**
     * <h3>페이지 정보 검증</h3>
     * <p>페이징 파라미터의 유효성을 검증</p>
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return boolean 유효성 여부
     * @author Jaeik
     * @since 2.0.0
     */
    protected boolean isValidPageParams(int page, int size) {
        return page >= 0 && size > 0 && size <= 100; // 최대 100개로 제한
    }
}