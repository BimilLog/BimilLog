package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.comment.infrastructure.adapter.out.persistence.CommentReadRepository;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.event.PostDeletedEvent;
import jaeik.growfarm.global.event.UserWithdrawnEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>댓글 서비스</h2>
 * <p>
 * 댓글 관련 Command 및 Query 유스케이스를 구현하는 서비스 클래스
 * </p>
 * <p>
 * 댓글 CRUD, 좋아요, 인기 댓글 조회 등 다양한 댓글 관련 기능을 제공
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentService implements CommentCommandUseCase, CommentQueryUseCase {

    private final LoadCommentPort loadCommentPort;
    private final SaveCommentPort saveCommentPort;
    private final DeleteCommentPort deleteCommentPort;
    private final SaveCommentLikePort saveCommentLikePort;
    private final LoadCommentLikePort loadCommentLikePort;
    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryUseCase userQueryUseCase;
    private final LoadPostPort loadPostPort;
    private final CommentReadRepository commentReadRepository;
    private final CommentDomainService commentDomainService;


    // ============== Query ==============

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails) {
        List<Long> likedCommentIds = getUserLikedCommentIdsForPopular(postId, userDetails);
        List<CommentDTO> popularComments = commentReadRepository.findPopularComments(postId, likedCommentIds);

        if (!popularComments.isEmpty()) {
            List<Long> commentIds = popularComments.stream().map(CommentDTO::getId).collect(Collectors.toList());
            Map<Long, Long> likeCounts = loadCommentLikePort.countByCommentIds(commentIds);
            popularComments.forEach(comment -> comment.setLikes(likeCounts.getOrDefault(comment.getId(), 0L).intValue()));
        }
        return popularComments;
    }

    /**
     * <h3>인기 댓글에 대한 사용자 좋아요 ID 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIdsForPopular(Long postId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }
        // 이 부분은 개선의 여지가 있습니다. 인기 댓글 ID를 먼저 가져오고, 그 ID들로 좋아요 여부를 확인하는 것이 더 효율적입니다.
        // 현재는 postId 전체 댓글에 대해 좋아요 여부를 확인하게 될 수 있습니다.
        return loadCommentPort.findUserLikedCommentIdsByPostId(postId, userDetails.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        List<Long> likedCommentIds = getUserLikedCommentIdsByPage(postId, pageable, userDetails);
        Page<CommentDTO> commentPage = commentReadRepository.findCommentsWithLatestOrder(postId, pageable, likedCommentIds);

        if (commentPage.hasContent()) {
            List<Long> commentIds = commentPage.getContent().stream().map(CommentDTO::getId).collect(Collectors.toList());
            Map<Long, Long> likeCounts = loadCommentLikePort.countByCommentIds(commentIds);
            commentPage.getContent().forEach(comment -> comment.setLikes(likeCounts.getOrDefault(comment.getId(), 0L).intValue()));
        }
        return commentPage;
    }

    /**
     * <h3>페이지별 사용자 좋아요 ID 조회</h3>
     * <p>주어진 게시글 ID와 페이지 정보에 해당하는 댓글 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIdsByPage(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }
        // 이 또한 comment ID 목록을 먼저 가져온 후 좋아요 여부를 확인하는 것이 더 효율적입니다.
        return loadCommentPort.findUserLikedCommentIdsByPostId(postId, userDetails.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long commentId) {
        return loadCommentPort.findById(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<jaeik.growfarm.dto.comment.SimpleCommentDTO> getUserComments(Long userId, Pageable pageable) {
        return commentReadRepository.findCommentsByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<jaeik.growfarm.dto.comment.SimpleCommentDTO> getUserLikedComments(Long userId, Pageable pageable) {
        return commentReadRepository.findLikedCommentsByUserId(userId, pageable);
    }


    // ============== Command ==============

    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDto) {
        Post post = loadPostPort.findById(commentDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        User user = null;
        if (userDetails != null) {
            user = userQueryUseCase.findById(userDetails.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        commentDomainService.saveCommentWithClosure(
                post,
                user,
                commentDto.getContent(),
                commentDto.getPassword(),
                commentDto.getParentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    this,
                    post.getUser().getId(),
                    commentDto.getUserName(),
                    commentDto.getPostId()));
        }
    }

    @Override
    public void updateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        comment.updateComment(commentDto.getContent());
        saveCommentPort.save(comment);
    }

    /**
     * <h3>댓글 유효성 검사 및 조회</h3>
     * <p>댓글 DTO와 사용자 인증 정보를 기반으로 댓글의 유효성을 검사하고 댓글 엔티티를 조회합니다.</p>
     * <p>비밀번호가 일치하지 않거나, 사용자 본인이 아닌 경우 예외를 발생시킵니다.</p>
     *
     * @param commentDto  댓글 DTO
     * @param userDetails 사용자 인증 정보
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @throws CustomException 댓글을 찾을 수 없거나, 비밀번호가 일치하지 않거나, 사용자 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = loadCommentPort.findById(commentDto.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentDto.getPassword() != null && !Objects.equals(comment.getPassword(), commentDto.getPassword())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentDto.getPassword() == null
                && (userDetails == null || !Objects.equals(comment.getUser().getId(), userDetails.getUserId()))) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
    }

    @Override
    public void deleteComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDto, userDetails);
        commentDomainService.deleteComment(comment);
    }

    @Override
    public void likeComment(CommentDTO commentDto, CustomUserDetails userDetails) {
        Long commentId = commentDto.getId();
        Long userId = userDetails.getUserId();

        Comment comment = loadCommentPort.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        User user = userQueryUseCase.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (loadCommentPort.isLikedByUser(commentId, userId)) {
            deleteCommentPort.deleteLike(comment, user);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            saveCommentLikePort.save(commentLike);
        }
    }

    /**
     * <h3>사용자 댓글 익명화</h3>
     * <p>특정 사용자가 작성한 모든 댓글을 익명화 처리합니다. (사용자 탈퇴 시 호출)</p>
     *
     * @param userId 익명화할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public void anonymizeUserComments(Long userId) {
        saveCommentPort.anonymizeUserComments(userId);
    }

    /**
     * <h3>사용자가 좋아요한 댓글 ID 목록 조회</h3>
     * <p>주어진 댓글 ID 목록 중 사용자가 좋아요를 누른 댓글의 ID 목록을 조회합니다.</p>
     *
     * @param commentIds  댓글 ID 목록
     * @param userDetails 사용자 인증 정보
     * @return List<Long> 사용자가 좋아요를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIds(List<Long> commentIds, CustomUserDetails userDetails) {
        return (userDetails != null)
                ? loadCommentPort.findUserLikedCommentIds(commentIds, userDetails.getUserId())
                : List.of();
    }

    /**
     * <h3>사용자 탈퇴 이벤트 핸들러</h3>
     * <p>사용자 탈퇴 이벤트를 수신하여 해당 사용자의 댓글을 익명화 처리합니다.</p>
     *
     * @param event 사용자 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("User (ID: {}) withdrawn event received. Anonymizing comments.", event.userId());
        anonymizeUserComments(event.userId());
    }

    /**
     * <h3>게시글 삭제 이벤트 핸들러</h3>
     * <p>게시글 삭제 이벤트를 수신하여 해당 게시글의 모든 댓글을 삭제합니다.</p>
     *
     * @param event 게시글 삭제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post (ID: {}) deleted event received. Deleting all comments.", event.postId());
        deleteCommentPort.deleteAllByPostId(event.postId());
    }
}
