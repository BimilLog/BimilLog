package jaeik.bimillog.infrastructure.adapter.user.out.persistence.comment;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.user.application.port.out.UserToCommentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 조회 어댑터</h2>
 * <p>사용자 도메인에서 댓글 도메인으로의 출력 어댑터입니다.</p>
 * <p>헥사고날 아키텍처 원칙에 따라 도메인 간 의존성을 올바르게 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserToCommentAdapter implements UserToCommentPort {

    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserComments(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentInfo> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserLikedComments(userId, pageable);
    }

}