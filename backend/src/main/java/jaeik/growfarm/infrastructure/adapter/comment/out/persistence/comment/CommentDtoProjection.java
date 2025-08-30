package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.comment.entity.QCommentClosure;
import jaeik.growfarm.domain.comment.entity.QCommentLike;
import jaeik.growfarm.domain.user.entity.QUser;
import jaeik.growfarm.domain.comment.entity.CommentInfo;
import jaeik.growfarm.domain.comment.entity.SimpleCommentInfo;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h2>댓글 DTO 프로젝션</h2>
 * <p>댓글 관련 데이터 전송 객체(DTO) 프로젝션 클래스</p>
 * <p>반환데이터를 프로젝션하는 클래스를 선언합니다.</p>
 * <p>comment 도메인의 DTO를 담당하며 CommentDTO SimpleCommentDTO를 담당합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@RequiredArgsConstructor
public class CommentDtoProjection {

    private static final QComment comment = QComment.comment;
    private static final QCommentLike commentLike = QCommentLike.commentLike;
    private static final QCommentClosure closure = QCommentClosure.commentClosure;
    private static final QUser user = QUser.user;


    /**
     * <h3>SimpleCommentDTO DTO 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>SimpleCommentDTO로 변환하는 프로젝션 - 서브쿼리로 사용자별 추천 여부를 한번에 계산</p>
     *
     * @param userId 사용자 ID (null인 경우 userLike는 false)
     * @return ConstructorExpression<SimpleCommentDTO> 댓글 DTO 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<SimpleCommentDTO> getSimpleCommentDtoProjection(Long userId) {
        return Projections.constructor(SimpleCommentDTO.class,
                comment.id,
                comment.post.id,
                user.userName,
                comment.content,
                comment.createdAt,
                commentLike.countDistinct().coalesce(0L).intValue(), // 실제 추천 수 계산
                userId != null ? 
                    JPAExpressions.selectOne()
                        .from(QCommentLike.commentLike)
                        .where(QCommentLike.commentLike.comment.id.eq(comment.id)
                            .and(QCommentLike.commentLike.user.id.eq(userId)))
                        .exists()
                    : Expressions.constant(false)
        );
    }

    /**
     * <h3>CommentDTO DTO 프로젝션</h3>
     * <p>CommentDTO로 변환하는 프로젝션</p>
     *
     * @return ConstructorExpression<CommentDTO> 댓글 DTO 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<CommentDTO> getCommentDtoProjection() {
        return Projections.constructor(CommentDTO.class,
                comment.id,
                comment.post.id,
                comment.user.id,
                user.userName,
                comment.content,
                comment.deleted,
                comment.createdAt,
                closure.ancestor.id,
                commentLike.countDistinct().coalesce(0L).intValue()
        );
    }

    // ===== 도메인 객체용 프로젝션 메서드들 =====

    /**
     * <h3>SimpleCommentInfo 도메인 객체 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>SimpleCommentInfo로 변환하는 프로젝션 - 서브쿼리로 사용자별 추천 여부를 한번에 계산</p>
     *
     * @param userId 사용자 ID (null인 경우 userLike는 false)
     * @return ConstructorExpression<SimpleCommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<SimpleCommentInfo> getSimpleCommentInfoProjection(Long userId) {
        return Projections.constructor(SimpleCommentInfo.class,
                comment.id,
                comment.post.id,
                user.userName,
                comment.content,
                comment.createdAt,
                commentLike.countDistinct().coalesce(0L).intValue(), // 실제 추천 수 계산
                userId != null ? 
                    JPAExpressions.selectOne()
                        .from(QCommentLike.commentLike)
                        .where(QCommentLike.commentLike.comment.id.eq(comment.id)
                            .and(QCommentLike.commentLike.user.id.eq(userId)))
                        .exists()
                    : Expressions.constant(false)
        );
    }

    /**
     * <h3>CommentInfo 도메인 객체 프로젝션</h3>
     * <p>CommentInfo로 변환하는 프로젝션</p>
     *
     * @return ConstructorExpression<CommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<CommentInfo> getCommentInfoProjection() {
        return Projections.constructor(CommentInfo.class,
                comment.id,
                comment.post.id,
                comment.user.id,
                user.userName,
                comment.content,
                comment.deleted,
                comment.createdAt,
                closure.ancestor.id,
                commentLike.countDistinct().coalesce(0L).intValue()
        );
    }

    /**
     * <h3>CommentInfo 도메인 객체 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>CommentInfo로 변환하는 프로젝션 - 서브쿼리로 사용자별 추천 여부를 한번에 계산</p>
     *
     * @param userId 사용자 ID (null인 경우 userLike는 false)
     * @return ConstructorExpression<CommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<CommentInfo> getCommentInfoProjectionWithUserLike(Long userId) {
        return Projections.constructor(CommentInfo.class,
                comment.id,
                comment.post.id,
                comment.user.id,
                user.userName,
                comment.content,
                comment.deleted,
                comment.createdAt,
                closure.ancestor.id,
                commentLike.countDistinct().coalesce(0L).intValue(),
                userId != null ? 
                    JPAExpressions.selectOne()
                        .from(QCommentLike.commentLike)
                        .where(QCommentLike.commentLike.comment.id.eq(comment.id)
                            .and(QCommentLike.commentLike.user.id.eq(userId)))
                        .exists()
                    : Expressions.constant(false)
        );
    }
}
