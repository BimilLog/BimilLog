package jaeik.growfarm.service;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.notification.FcmSendDTO;
import jaeik.growfarm.entity.board.Comment;
import jaeik.growfarm.entity.board.CommentLike;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.notification.FcmToken;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.jwt.CustomUserDetails;
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


    // 댓글 작성
    public void writeComment(Long postId, CommentDTO commentDTO) throws IOException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        Users user = userRepository.findById(commentDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + commentDTO.getUserId()));

        Long postUserId = post.getUser().getId();

        commentRepository.save(boardUtil.commentDTOToComment(commentDTO, post, user));
        notificationService.send(postUserId, notificationUtil.createEventDTO(NotificationType.COMMENT, user.getFarmName() + "님이 댓글을 남겼습니다!", "https://grow-farm.com/board/" + postId));

        if (post.getUser().getSetting().isCommentNotification()) {
            List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(post.getUser());
            for (FcmToken fcmToken : fcmTokens) {
                notificationService.sendMessageTo(FcmSendDTO.builder()
                        .token(fcmToken.getFcmRegistrationToken())
                        .title(user.getFarmName() + "님이 댓글을 남겼습니다!")
                        .body("지금 확인해보세요!")
                        .build()
                );
            }
        }
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long commentId, CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(userDetails.getUserDTO().getUserId())) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }
        comment.updateComment(commentDTO.getContent());
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, CustomUserDetails customUserDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        if (!comment.getUser().getId().equals(customUserDetails.getUserDTO().getUserId())) {
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

        Users user = userRepository.findById(userDetails.getUserDTO().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userDetails.getUserDTO().getUserId()));

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId,
                userDetails.getUserDTO().getUserId());

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

    public void reportComment(Long postId, Long commentId, CustomUserDetails userDetails, String content) {
        Report report = Report.builder()
                .users(userUtil.DTOToUser(userDetails.getUserDTO()))
                .reportType(ReportType.COMMENT)
                .targetId(commentId)
                .content(content)
                .build();

        reportRepository.save(report);
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
                                list -> list.stream().limit(3).toList()
                        )
                ));

        // Step 4: 인기 댓글 지정 및 알림 전송
        topCommentsByPost.values().stream()
                .flatMap(List::stream)
                .forEach(comment -> {
                    comment.setIsFeatured(true); // 인기 댓글 지정

                    Long userId = comment.getUser().getId();
                    Long postId = comment.getPost().getId();

                    // 알림 설정 확인 및 FCM 전송
                    if (comment.getUser().getSetting().isCommentNotification()) {
                        List<FcmToken> fcmTokens = fcmTokenRepository.findByUsers(comment.getUser());
                        for (FcmToken fcmToken : fcmTokens) {
                            try {
                                notificationService.sendMessageTo(FcmSendDTO.builder()
                                        .token(fcmToken.getFcmRegistrationToken())
                                        .title("🎉 당신의 댓글이 인기 댓글로 선정되었습니다!")
                                        .body("지금 확인해보세요!")
                                        .build()
                                );
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
                                    "https://grow-farm.com/board" + postId
                            )
                    );
                });
    }

}
