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

@Repository
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements
        LoadCommentPort, SaveCommentPort, DeleteCommentPort,
        LoadCommentLikePort, SaveCommentLikePort, DeleteCommentLikePort,
        LoadCommentClosurePort, SaveCommentClosurePort, DeleteCommentClosurePort {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentClosureRepository commentClosureRepository;
    private final CommentReadRepository commentReadRepository; // QueryDSL 구현체

    // ================== LoadCommentPort ==================
    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    @Override
    public Long countRootCommentsByPostId(Long postId) {
        return commentReadRepository.countRootCommentsByPostId(postId);
    }

    @Override
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentReadRepository.findCommentCountsByPostIds(postIds);
    }

    @Override
    public List<Long> findUserLikedCommentIds(List<Long> commentIds, Long userId) {
        return commentRepository.findUserLikedCommentIds(commentIds, userId);
    }

    @Override
    public Page<SimpleCommentDTO> findCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findCommentsByUserId(userId, pageable);
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
    }
    
    @Override
    public List<Comment> findByPost(Post post) {
        return commentRepository.findByPost(post);
    }

    @Override
    public boolean isLikedByUser(Long commentId, Long userId) {
        // 이 로직은 userId와 commentId만으로 처리하는 것이 더 효율적일 수 있습니다.
        // 현재는 CommentService에서 이미 User와 Comment 엔티티를 조회하고 있어,
        // 그 엔티티를 활용하는 것이 추가적인 쿼리를 줄일 수 있습니다.
        // 하지만 포트의 의미를 명확히 하기 위해 이대로 구현합니다.
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId).isPresent();
    }

    @Override
    public List<Long> findUserLikedCommentIdsByPostId(Long postId, Long userId) {
        return commentRepository.findUserLikedCommentIdsByPostId(postId, userId);
    }

    // ================== SaveCommentPort ==================
    @Override
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public void anonymizeUserComments(Long userId) {
        commentRepository.anonymizeUserComments(userId);
    }

    // ================== DeleteCommentPort ==================
    @Override
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }

    @Override
    public void deleteById(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteLike(Comment comment, User user) {
        commentLikeRepository.deleteByCommentAndUser(comment, user);
    }

    @Override
    public void deleteAllByPostId(Long postId) {
        commentRepository.deleteAllByPostId(postId);
    }

    // ================== LoadCommentLikePort ==================
    @Override
    public Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId) {
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
    }

    @Override
    public Map<Long, Long> countByCommentIds(List<Long> commentIds) {
        return commentLikeRepository.countByCommentIds(commentIds);
    }

    // ================== SaveCommentLikePort ==================
    @Override
    public CommentLike save(CommentLike commentLike) {
        return commentLikeRepository.save(commentLike);
    }

    // ================== DeleteCommentLikePort ==================
    @Override
    public void delete(CommentLike commentLike) {
        commentLikeRepository.delete(commentLike);
    }

    // ================== LoadCommentClosurePort ==================
    @Override
    public Optional<List<CommentClosure>> findByDescendantId(Long descendantId) {
        return commentClosureRepository.findByDescendantId(descendantId);
    }

    @Override
    public boolean hasDescendants(Long commentId) {
        return commentClosureRepository.hasDescendants(commentId);
    }

    // ================== SaveCommentClosurePort ==================
    @Override
    public void save(CommentClosure commentClosure) {
        commentClosureRepository.save(commentClosure);
    }

    // ================== DeleteCommentClosurePort ==================
    @Override
    public void deleteByDescendantId(Long commentId) {
        commentClosureRepository.deleteByDescendantId(commentId);
    }

    @Override
    public void deleteByDescendantIds(List<Long> commentIds) {
        commentClosureRepository.deleteByDescendantIds(commentIds);
    }
}
