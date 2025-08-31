package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.ReportedUserResolver;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 신고 사용자 해결사</h2>
 * <p>댓글 신고 유형에 대해 신고 대상 사용자 정보를 해결하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentReportedUserResolver implements ReportedUserResolver {

    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * <h3>댓글 ID로 신고 대상 사용자 해결</h3>
     * <p>주어진 댓글 ID에 해당하는 댓글의 작성자(사용자)를 조회하여 반환합니다.</p>
     *
     * @param targetId 댓글 ID
     * @return User 댓글 작성 사용자 엔티티
     * @throws CustomException 댓글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User resolve(Long targetId) {
        Comment comment = commentQueryUseCase.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_FAILED));
        return comment.getUser();
    }

    /**
     * <h3>지원하는 신고 유형 반환</h3>
     * <p>이 해결사가 지원하는 신고 유형(COMMENT)을 반환합니다.</p>
     *
     * @return ReportType.COMMENT
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ReportType supports() {
        return ReportType.COMMENT;
    }
}
