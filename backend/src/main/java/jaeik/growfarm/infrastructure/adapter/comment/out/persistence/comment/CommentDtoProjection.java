package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import jaeik.growfarm.domain.comment.entity.QComment;
import jaeik.growfarm.domain.user.entity.QUser;
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
    private static final QUser user = QUser.user;


    /**
     * <p>{@link SimpleCommentDTO}를 위한 {@code ConstructorExpression}을 반환합니다.</p>
     * <p>사용자가 추천한 댓글 목록 조회 시 {@code SELECT} 절에 사용됩니다.</p>
     *
     * @return SimpleCommentDTO를 생성하는 {@code ConstructorExpression}
     */
    public static ConstructorExpression<SimpleCommentDTO> getSimpleCommentDtoProjection() {
        return Projections.constructor(SimpleCommentDTO.class,
                comment.id,
                comment.post.id,
                user.userName,
                comment.content,
                comment.createdAt,
                Expressions.constant(0),
                Expressions.constant(false));
    }


}
