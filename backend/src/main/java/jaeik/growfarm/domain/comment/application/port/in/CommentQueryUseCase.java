package jaeik.growfarm.domain.comment.application.port.in;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 조회 유스케이스</h2>
 * <p>댓글 조회 관련 요청을 처리하는 인터페이스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryUseCase {

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글의 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<CommentDTO> 인기 댓글 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails);

    /**
     * <h3>최신순 댓글 조회</h3>
     * <p>주어진 게시글의 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param page        페이지 번호
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails);

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Comment> findById(Long commentId);

}
