package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import jaeik.growfarm.domain.comment.entity.Comment;

public interface CommentQueryUseCase {

    List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails);

    Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails);

    Optional<Comment> findById(Long commentId);

    /**
     * <h3>사용자 작성 댓글 목록 조회 (도메인 간 연동용)</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * 
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 작성한 댓글 목록 페이지
     */
    Page<SimpleCommentDTO> getUserComments(Long userId, Pageable pageable);

    /**
     * <h3>사용자 좋아요한 댓글 목록 조회 (도메인 간 연동용)</h3>
     * <p>특정 사용자가 좋아요한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * 
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return 좋아요한 댓글 목록 페이지
     */
    Page<SimpleCommentDTO> getUserLikedComments(Long userId, Pageable pageable);
}
