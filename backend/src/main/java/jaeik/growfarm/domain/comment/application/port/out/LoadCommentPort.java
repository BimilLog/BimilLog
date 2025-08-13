package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>댓글 조회 포트</h2>
 * <p>댓글 엔티티 조회를 위한 Out-Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadCommentPort {

    /**
     * <h3>게시글로 댓글 목록 조회</h3>
     * <p>주어진 게시글에 속한 모든 댓글 목록을 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return List<Comment> 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Comment> findByPost(Post post);

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
     * <h3>게시글 ID로 루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    Long countRootCommentsByPostId(Long postId);

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);

    /**
     * <h3>사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
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
     * <h3>사용자 좋아요한 댓글 목록 조회</h3>
     * <p>특정 사용자가 좋아요한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 좋아요한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable);

    /**
     * <h3>사용자가 댓글에 좋아요를 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 좋아요 관계인지 확인합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 좋아요를 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isLikedByUser(Long commentId, Long userId);

    /**
     * <h3>게시글 ID로 사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId);
}
