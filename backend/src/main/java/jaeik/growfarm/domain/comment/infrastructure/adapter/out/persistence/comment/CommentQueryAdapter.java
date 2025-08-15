package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.comment;

import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>댓글 쿼리 어댑터</h2>
 *
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentQueryAdapter implements CommentQueryPort {

    private final CommentRepository commentRepository;
    private final CommentReadRepository commentReadRepository;

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    /**
     * <h3>게시글 ID로 루트 댓글 수 조회</h3>
     * <p>주어진 게시글 ID에 해당하는 최상위(루트) 댓글의 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Long 루트 댓글의 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Long countRootCommentsByPostId(Long postId) {
        return commentReadRepository.countRootCommentsByPostId(postId);
    }

    /**
     * <h3>여러 게시글 ID에 대한 댓글 수 조회</h3>
     * <p>주어진 여러 게시글 ID에 해당하는 각 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentReadRepository.findCommentCountsByPostIds(postIds);
    }

    /**
     * <h3>사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId) {
        return commentRepository.findUserLikedCommentIds(commentIds, userId);
    }

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
    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findCommentsByUserId(userId, pageable);
    }

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
    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
    }
    



    /**
     * <h3>게시글 ID로 사용자가 추천한 댓글 ID 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId) {
        return commentRepository.findUserLikedCommentIdsByPostId(postId, userId);
    }

    /**
     * <h3>인기 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return List<CommentDTO> 인기 댓글 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds) {
        return commentReadRepository.findPopularComments(postId, likedCommentIds);
    }

    /**
     * <h3>최신순 댓글 목록 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @param pageable 페이지 정보
     * @param likedCommentIds 사용자가 추천한 댓글 ID 목록
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds) {
        return commentReadRepository.findCommentsWithLatestOrder(postId, pageable, likedCommentIds);
    }

}
