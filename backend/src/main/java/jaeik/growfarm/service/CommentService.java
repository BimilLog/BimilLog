package jaeik.growfarm.service;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentLike;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.event.CommentCreatedEvent;
import jaeik.growfarm.event.CommentFeaturedEvent;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.notification.FcmService;
import jaeik.growfarm.service.notification.SseService;
import jaeik.growfarm.service.notification.NotificationService;
import jaeik.growfarm.util.BoardUtil;
import jaeik.growfarm.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * 댓글 서비스
 * 댓글 작성, 수정, 삭제, 추천, 신고 기능을 제공하는 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BoardUtil boardUtil;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;
    private final FcmTokenRepository fcmTokenRepository;
    private final SseService sseService; // 비동기 SSE 알림 서비스
    private final FcmService fcmService; // 비동기 FCM 알림 서비스

    // 이벤트 발행을 위한 ApplicationEventPublisher 🚀
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>댓글 작성</h3>
     *
     * <p>
     * 댓글을 DB에 저장하고 글 작성자에게 실시간 알림과 푸시 메시지를 발송한다.
     * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postId      게시글 ID
     * @param commentDTO  댓글 정보 DTO
     * @throws IOException FCM 메시지 발송 오류 시 발생
     */
    public void writeComment(CustomUserDetails userDetails, Long postId, CommentDTO commentDTO) throws IOException {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.NULL_SECURITY_CONTEXT);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_POST));

        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_MATCH_USER));

        Long postUserId = post.getUser().getId();

        // 댓글 저장 (동기)
        commentRepository.save(boardUtil.commentDTOToComment(commentDTO, post, user));

        // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
        eventPublisher.publishEvent(new CommentCreatedEvent(
                postUserId,
                user.getFarmName(),
                postId,
                post.getUser()));
    }

    /**
     * <h3>댓글 수정</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentId   댓글 ID
     * @param commentDTO  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     */
    @Transactional
    public void updateComment(Long commentId, CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }
        comment.updateComment(commentDTO.getContent());
    }

    /**
     * <h3>댓글 삭제</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param commentId         댓글 ID
     * @param customUserDetails 현재 로그인한 사용자 정보
     */
    @Transactional
    public void deleteComment(Long commentId, CustomUserDetails customUserDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(customUserDetails.getClientDTO().getUserId())) {
            throw new IllegalArgumentException("댓글 작성자만 삭제할 수 있습니다.");
        }

        commentLikeRepository.deleteAllByCommentId(commentId);
        commentRepository.delete(comment);
    }

    // 댓글 추천, 추천 취소
    public void likeComment(Long postId, Long commentId, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        Users user = userRepository.findById(userDetails.getClientDTO().getUserId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다: " + userDetails.getClientDTO().getUserId()));

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId,
                userDetails.getClientDTO().getUserId());

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();

            commentLikeRepository.save(commentLike);
        }
    }

    /**
     * <h3>인기댓글 업데이트 (이벤트 기반 비동기 처리)</h3>
     *
     * <p>
     * 5분마다 추천 수 3개 이상인 댓글을 인기댓글로 선정하고,
     * 이벤트 발행을 통해 SSE와 FCM 알림을 비동기로 처리한다.
     * </p>
     */
    @Transactional
    @Scheduled(fixedRate = 1000 * 60 * 5) // 5분마다 실행
    public void updateFeaturedComments() {
        // Step 1: 기존 인기 댓글 초기화
        commentRepository.resetAllCommentFeaturedFlags();

        // Step 2: 추천 수 3개 이상인 댓글 전부 불러오기
        List<Comment> popularComments = commentRepository.findPopularComments();

        // Step 3: 게시글별 상위 3개만 선정
        Map<Long, List<Comment>> topCommentsByPost = popularComments.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getPost().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().limit(3).toList())));

        // Step 4: 인기 댓글 지정 및 이벤트 발행 🚀
        topCommentsByPost.values().stream()
                .flatMap(List::stream)
                .forEach(comment -> {
                    comment.updatePopular(true); // 인기 댓글 지정

                    // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
                    eventPublisher.publishEvent(new CommentFeaturedEvent(
                            comment.getUser().getId(),
                            comment.getPost().getId(),
                            comment.getUser()));
                });
    }
}
