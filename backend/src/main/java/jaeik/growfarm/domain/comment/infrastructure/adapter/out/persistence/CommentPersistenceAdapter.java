package jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence;

import com.querydsl.core.Tuple;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.domain.Comment;
import jaeik.growfarm.domain.comment.domain.CommentClosure;
import jaeik.growfarm.domain.comment.domain.CommentLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import org.springframework.data.domain.Page;

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
    public List<Tuple> findCommentsWithLatestOrder(Long postId, Pageable pageable) {
        return commentReadRepository.findCommentsWithLatestOrder(postId, pageable);
    }

    @Override
    public List<Tuple> findPopularComments(Long postId) {
        return commentReadRepository.findPopularComments(postId);
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
        return null;
    }

    @Override
    public Page<SimpleCommentDTO> findLikedCommentsByUserId(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
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

    // ================== LoadCommentLikePort ==================
    @Override
    public Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId) {
        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
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
