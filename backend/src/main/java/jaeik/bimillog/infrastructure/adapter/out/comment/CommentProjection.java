package jaeik.bimillog.infrastructure.adapter.out.comment;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import jaeik.bimillog.domain.comment.entity.*;
import jaeik.bimillog.domain.member.entity.member.QMember;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h2>댓글 프로젝션</h2>
 * <p>댓글 관련 데이터 전송 객체 프로젝션 클래스</p>
 * <p>반환데이터를 프로젝션하는 클래스를 선언합니다.</p>
 * <p>comment 도메인을 담당합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@RequiredArgsConstructor
public class CommentProjection {

    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QCommentClosure closure = QCommentClosure.commentClosure;
    private static final QMember member = QMember.member;

    /**
     * <h3>SimpleCommentInfo 도메인 객체 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>SimpleCommentInfo로 변환하는 프로젝션 - 서브쿼리로 사용자별 추천 여부를 한번에 계산</p>
     *
     * @param memberId 사용자 ID (null인 경우 userLike는 false)
     * @return ConstructorExpression<SimpleCommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<SimpleCommentInfo> getSimpleCommentInfoProjection(Long memberId) {
        return Projections.constructor(SimpleCommentInfo.class,
                comment.id,
                comment.post.id,
                member.memberName,
                comment.content,
                comment.createdAt,
                commentLike.countDistinct().coalesce(0L).intValue(), // 실제 추천 수 계산
                memberId != null ?
                    JPAExpressions.selectOne()
                        .from(QCommentLike.commentLike)
                        .where(QCommentLike.commentLike.comment.id.eq(comment.id)
                            .and(QCommentLike.commentLike.member.id.eq(memberId)))
                        .exists()
                    : Expressions.constant(false)
        );
    }

    /**
     * <h3>CommentInfo 도메인 객체 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>CommentInfo로 변환하는 프로젝션 - 서브쿼리로 사용자별 추천 여부를 한번에 계산</p>
     *
     * @param memberId 사용자 ID (null인 경우 userLike는 false)
     * @return ConstructorExpression<CommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<CommentInfo> getCommentInfoProjectionWithUserLike(Long memberId) {
        return Projections.constructor(CommentInfo.class,
                comment.id,
                comment.post.id,
                comment.member.id,
                member.memberName,
                comment.content,
                comment.deleted,
                comment.createdAt,
                // parentId: depth=1인 closure에서 ancestor.id를 가져오거나, 없으면 자기 자신의 id
                JPAExpressions.select(closure.ancestor.id.coalesce(comment.id))
                    .from(closure)
                    .where(closure.descendant.id.eq(comment.id)
                        .and(closure.depth.eq(1)))
                    .limit(1),
                commentLike.countDistinct().coalesce(0L).intValue(),
                memberId != null ?
                    JPAExpressions.selectOne()
                        .from(QCommentLike.commentLike)
                        .where(QCommentLike.commentLike.comment.id.eq(comment.id)
                            .and(QCommentLike.commentLike.member.id.eq(memberId)))
                        .exists()
                    : Expressions.constant(false)
        );
    }
}
