package jaeik.bimillog.domain.comment.out;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import jaeik.bimillog.domain.comment.entity.*;
import jaeik.bimillog.domain.member.entity.QMember;
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
     * <p>SimpleCommentInfo로 변환하는 프로젝션 - 조인된 userCommentLike로 추천 여부 확인</p>
     *
     * @param userCommentLike 사용자별 좋아요 조인 엔티티 (메인 쿼리에서 조인 필요)
     * @return ConstructorExpression<SimpleCommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<SimpleCommentInfo> getSimpleCommentInfoProjection(QCommentLike userCommentLike) {
        return Projections.constructor(SimpleCommentInfo.class,
                comment.id,
                comment.post.id,
                member.memberName,
                comment.content,
                comment.createdAt,
                commentLike.countDistinct().coalesce(0L).intValue(), // 실제 추천 수 계산
                userCommentLike.id.isNotNull() // 조인된 userCommentLike 존재 여부로 판단
        );
    }

    /**
     * <h3>SimpleCommentInfo 도메인 객체 프로젝션 (전체 좋아요 카운트 포함)</h3>
     * <p>추천한 댓글 조회용 프로젝션 - 별도 QCommentLike로 전체 좋아요 수 정확히 카운트</p>
     * <p>WHERE 절로 필터링하면 GROUP BY 전에 좋아요가 줄어들어 카운트가 부정확해지므로,</p>
     * <p>전체 좋아요 카운트용 별도 LEFT JOIN을 사용</p>
     *
     * @param allLikes 전체 좋아요 카운트용 조인 엔티티 (메인 쿼리에서 LEFT JOIN 필요)
     * @param userCommentLike 사용자별 좋아요 조인 엔티티 (메인 쿼리에서 조인 필요)
     * @return ConstructorExpression<SimpleCommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<SimpleCommentInfo> getSimpleCommentInfoProjectionWithAllLikes(
            QCommentLike allLikes,
            QCommentLike userCommentLike) {
        return Projections.constructor(SimpleCommentInfo.class,
                comment.id,
                comment.post.id,
                member.memberName,
                comment.content,
                comment.createdAt,
                allLikes.countDistinct().coalesce(0L).intValue(), // 전체 좋아요 카운트
                userCommentLike.id.isNotNull() // 사용자 추천 여부
        );
    }

    /**
     * <h3>CommentInfo 도메인 객체 프로젝션 (사용자별 추천 여부 포함)</h3>
     * <p>CommentInfo로 변환하는 프로젝션 - 조인된 엔티티를 직접 사용하여 N+1 문제 해결</p>
     *
     * @param parentClosure 부모 댓글 관계 조인 엔티티 (메인 쿼리에서 depth=1 조건으로 조인 필요)
     * @param userCommentLike 사용자별 좋아요 조인 엔티티 (메인 쿼리에서 조인 필요)
     * @return ConstructorExpression<CommentInfo> 댓글 도메인 객체 프로젝션
     * @author Jaeik
     * @since 2.0.0
     */
    public static ConstructorExpression<CommentInfo> getCommentInfoProjectionWithUserLike(
            QCommentClosure parentClosure,
            QCommentLike userCommentLike) {
        return Projections.constructor(CommentInfo.class,
                comment.id,
                comment.post.id,
                comment.member.id,
                member.memberName,
                comment.content,
                comment.deleted,
                comment.createdAt,
                // parentId: 조인된 parentClosure에서 ancestor.id 직접 참조 (서브쿼리 제거)
                parentClosure.ancestor.id.coalesce(comment.id),
                commentLike.countDistinct().coalesce(0L).intValue(),
                // userLike: 조인된 userCommentLike 존재 여부로 판단 (서브쿼리 제거)
                userCommentLike.id.isNotNull()
        );
    }
}
