package jaeik.growfarm.service;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.board.Comment;
import jaeik.growfarm.entity.board.CommentLike;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.notification.FcmTokenRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.BoardUtil;
import jaeik.growfarm.util.NotificationUtil;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
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
    private final ReportRepository reportRepository;
    private final BoardUtil boardUtil;
    private final UserUtil userUtil;
    private final NotificationService notificationService;
    private final NotificationUtil notificationUtil;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * <h3>댓글 작성</h3>
     *
     * <p>
     * 댓글을 DB에 저장하고 글 작성자에게 실시간 알림과 푸시 메시지를 발송한다.
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

        commentRepository.save(boardUtil.commentDTOToComment(commentDTO, post, user));
        notificationService.send(postUserId, notificationUtil.createEventDTO(NotificationType.COMMENT,
                user.getFarmName() + "님이 댓글을 남겼습니다!", "http://localhost:3000/board/" + postId));

        if (post.getUser().getSetting().commentNotification()) {
            List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(post.getUser());
            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(user.getFarmName() + "님이 댓글을 남겼습니다!")
                        .body("지금 확인해보세요!")
                        .build());
            }
        }
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
                        () -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userDetails.getClientDTO().getUserId()));

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

        // Step 4: 인기 댓글 지정 및 알림 전송
        topCommentsByPost.values().stream()
                .flatMap(List::stream)
                .forEach(comment -> {
                    comment.setIsFeatured(true); // 인기 댓글 지정

                    Long userId = comment.getUser().getId();
                    Long postId = comment.getPost().getId();

                    // 알림 설정 확인 및 FCM 전송
                    if (comment.getUser().getSetting().commentNotification()) {
                        List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(comment.getUser());
                        for (FcmToken fcmToken : fcmTokens) {
                            try {
                                notificationService.sendMessageTo(FcmSendDTO.builder()
                                        .token(fcmToken.getFcmRegistrationToken())
                                        .title("🎉 당신의 댓글이 인기 댓글로 선정되었습니다!")
                                        .body("지금 확인해보세요!")
                                        .build());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    // 일반 알림 전송 (웹용 등)
                    notificationService.send(
                            userId,
                            notificationUtil.createEventDTO(
                                    NotificationType.COMMENT_FEATURED,
                                    "🎉 당신의 댓글이 인기 댓글로 선정되었습니다!",
                                    "http://localhost:3000/board/" + postId));
                });
    }

}
