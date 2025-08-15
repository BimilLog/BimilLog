package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>댓글 엔티티 조회를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 조회 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentQueryPort {

    /**
     * <h3>인기 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId          게시글 ID
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return List<jaeik.growfarm.dto.comment.CommentDTO> 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds);

    /**
     * <h3>최신순 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId          게시글 ID
     * @param pageable        페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<jaeik.growfarm.dto.comment.CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds);

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param id 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Comment> findById(Long id);


    /**
     * <h3>사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 추천을 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 추천을 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId);

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);

}