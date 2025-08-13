package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.post.entity.Post;

/**
 * <h2>댓글 영속성 어댑터</h2>
 * <p>
 * 댓글 관련 데이터를 영속화하고 조회하는 Outgoing-Adapter
 * </p>
 * <p>
 * 다양한 댓글 관련 포트 인터페이스를 구현하며, 레포지토리와의 상호작용을 담당
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements
        LoadCommentPort, SaveCommentPort, DeleteCommentPort,
        LoadCommentLikePort, SaveCommentLikePort, DeleteCommentLikePort,
        LoadCommentClosurePort, SaveCommentClosurePort, DeleteCommentClosurePort,
        LoadCommentQueryPort {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentClosureRepository commentClosureRepository;
    private final CommentReadRepository commentReadRepository; // QueryDSL 구현체

    // ================== LoadCommentPort ==================
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
     * <h3>사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @param userId     사용자 ID
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
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
     * <h3>사용자 좋아요한 댓글 목록 조회</h3>
     * <p>특정 사용자가 좋아요한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 좋아요한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
    }
    
    /**
     * <h3>게시글로 댓글 목록 조회</h3>
     * <p>주어진 게시글에 속한 모든 댓글 목록을 조회합니다.</p>
     *
     * @param post 게시글 엔티티
     * @return List<Comment> 댓글 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Comment> findByPost(Post post) {
        return commentRepository.findByPost(post);
    }

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
    @Override
    public boolean isLikedByUser(Long commentId, Long userId) {
        // 이 로직은 userId와 commentId만으로 처리하는 것이 더 효율적일 수 있습니다.
        // 현재는 CommentService에서 이미 User와 Comment 엔티티를 조회하고 있어,
        // 그 엔티티를 활용하는 것이 추가적인 쿼리를 줄일 수 있습니다.
        // 하지만 포트의 의미를 명확히 하기 위해 이대로 구현합니다.
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId).isPresent();
    }

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
    @Override
    public List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId) {
        return commentRepository.findUserLikedCommentIdsByPostId(postId, userId);
    }

    // ================== SaveCommentPort ==================
    /**
     * <h3>댓글 저장</h3>
     * <p>주어진 댓글 엔티티를 저장합니다.</p>
     *
     * @param comment 저장할 댓글 엔티티
     * @return Comment 저장된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }

    // ================== DeleteCommentPort ==================
    /**
     * <h3>댓글 삭제</h3>
     * <p>주어진 댓글 엔티티를 삭제합니다.</p>
     *
     * @param comment 삭제할 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }

    /**
     * <h3>ID로 댓글 삭제</h3>
     * <p>주어진 ID의 댓글을 삭제합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteById(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    /**
     * <h3>댓글 좋아요 삭제</h3>
     * <p>주어진 댓글과 사용자의 좋아요 관계를 삭제합니다.</p>
     *
     * @param comment 좋아요를 삭제할 댓글 엔티티
     * @param user    좋아요를 삭제할 사용자 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteLike(Comment comment, User user) {
        commentLikeRepository.deleteByCommentAndUser(comment, user);
    }

    /**
     * <h3>게시글 ID로 모든 댓글 삭제</h3>
     * <p>주어진 게시글 ID에 해당하는 모든 댓글을 삭제합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllByPostId(Long postId) {
        commentRepository.deleteAllByPostId(postId);
    }

    // ================== LoadCommentLikePort ==================
    /**
     * <h3>댓글 ID와 사용자 ID로 댓글 좋아요 조회</h3>
     * <p>주어진 댓글 ID와 사용자 ID에 해당하는 댓글 좋아요 엔티티를 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return Optional<CommentLike> 조회된 댓글 좋아요 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId) {
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
    }

    /**
     * <h3>여러 댓글 ID에 대한 좋아요 수 조회</h3>
     * <p>주어진 댓글 ID 목록에 해당하는 각 댓글의 좋아요 수를 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @return Map<Long, Long> 댓글 ID를 키로, 좋아요 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<Long, Long> countByCommentIds(List<Long> commentIds) {
        return commentLikeRepository.countByCommentIds(commentIds);
    }

    // ================== SaveCommentLikePort ==================
    /**
     * <h3>댓글 좋아요 저장</h3>
     * <p>주어진 댓글 좋아요 엔티티를 저장합니다.</p>
     *
     * @param commentLike 저장할 댓글 좋아요 엔티티
     * @return CommentLike 저장된 댓글 좋아요 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public CommentLike save(CommentLike commentLike) {
        return commentLikeRepository.save(commentLike);
    }

    // ================== DeleteCommentLikePort ==================
    /**
     * <h3>댓글 좋아요 삭제</h3>
     * <p>주어진 댓글 좋아요 엔티티를 삭제합니다.</p>
     *
     * @param commentLike 삭제할 댓글 좋아요 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(CommentLike commentLike) {
        commentLikeRepository.delete(commentLike);
    }

    // ================== LoadCommentClosurePort ==================
    /**
     * <h3>자손 ID로 댓글 클로저 목록 조회</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티 목록을 조회합니다.</p>
     *
     * @param descendantId 자손 댓글 ID
     * @return Optional<List<CommentClosure>> 조회된 댓글 클로저 엔티티 목록. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }

    /**
     * <h3>댓글의 자손 존재 여부 확인</h3>
     * <p>주어진 댓글 ID를 조상으로 하는 자손 댓글이 존재하는지 확인합니다.</p>
     *
     * @param commentId 확인할 댓글 ID
     * @return boolean 자손 댓글이 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean hasDescendants(Long commentId) {
        return commentClosureRepository.hasDescendants(commentId);
    }

    // ================== SaveCommentClosurePort ==================
    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(CommentClosure commentClosure) {
        commentClosureRepository.save(commentClosure);
    }

    // ================== DeleteCommentClosurePort ==================
    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByDescendantId(Long commentId) {
        commentClosureRepository.deleteByDescendantId(commentId);
    }

    /**
     * <h3>여러 자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 여러 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentIds 여러 자손 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteByDescendantIds(List<Long> commentIds) {
        commentClosureRepository.deleteByDescendantIds(commentIds);
    }

    // ================== LoadCommentQueryPort ==================
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
    @Override
    public List<jaeik.growfarm.dto.comment.CommentDTO> findPopularComments(Long postId, List<Long> likedCommentIds) {
        return commentReadRepository.findPopularComments(postId, likedCommentIds);
    }

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
    @Override
    public Page<jaeik.growfarm.dto.comment.CommentDTO> findCommentsWithLatestOrder(Long postId, Pageable pageable, List<Long> likedCommentIds) {
        return commentReadRepository.findCommentsWithLatestOrder(postId, pageable, likedCommentIds);
    }
}
