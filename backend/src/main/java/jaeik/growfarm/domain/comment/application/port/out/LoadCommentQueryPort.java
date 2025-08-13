package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.dto.comment.CommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * <h2>댓글 특화 조회 포트</h2>
 * <p>댓글의 복잡한 쿼리 조회를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadCommentQueryPort {

    /**
     * <h3>인기 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param likedCommentIds 사용자가 좋아요한 댓글 ID 목록
     * @return List<CommentDTO> 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds);

    /**
     * <h3>최신순 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param pageable 페이지 정보
     * @param likedCommentIds 사용자가 좋아요한 댓글 ID 목록
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds);
}