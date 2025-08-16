package jaeik.growfarm.infrastructure.adapter.user.out.persistence.comment;

import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.LoadCommentPort;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 조회 어댑터</h2>
 * <p>User 도메인에서 Comment 도메인으로의 아웃바운드 어댑터입니다.</p>
 * <p>헥사고날 아키텍처 원칙에 따라 도메인 간 의존성을 올바르게 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LoadCommentAdapter implements LoadCommentPort {

    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserComments(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 Comment 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentQueryUseCase.getUserLikedComments(userId, pageable);
    }
}