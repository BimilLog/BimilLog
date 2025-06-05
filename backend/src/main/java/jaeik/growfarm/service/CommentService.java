package jaeik.growfarm.service;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentClosure;
import jaeik.growfarm.entity.comment.CommentLike;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.event.CommentFeaturedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>댓글 서비스</h2>
 * <p>
 * 댓글 작성, 수정, 삭제, 추천 기능을 제공하며,
 * 이벤트 기반 아키텍처로 실시간 알림과 푸시 메시지를 비동기로 처리한다.
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentClosureRepository commentClosureRepository;

    /**
     * <h3>댓글 작성</h3>
     *
     * <p>댓글을 DB에 저장하고 글 작성자에게 실시간 알림과 푸시 메시지를 발송한다.</p>
     * <p>이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.</p>
     * <p>클로저 테이블 패턴을 활용하여 계층형태로 저장한다.</p>
     * <p>댓글 저장이 성공해도 클로저 테이블에서 오류가 발생하면 롤백시킨다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param commentDTO  댓글 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDTO) {
        Post post = postRepository.getReferenceById(commentDTO.getPostId());
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;

        try {
            Comment comment = commentRepository.save(Comment.createComment(post, user, commentDTO.getContent(), commentDTO.getPassword()));
            CommentClosure commentClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureRepository.save(commentClosure);
            if (commentDTO.getParentId() != null) {
                List<CommentClosure> parentComments = commentClosureRepository.findByDescendantId(comment.getId()).orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
                for (CommentClosure parentComment : parentComments) {
                    Comment ancestor = parentComment.getAncestor();
                    int depth = parentComment.getDepth() + 1;
                    commentClosureRepository.save(CommentClosure.createCommentClosure(ancestor, comment, depth));
                }
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }

        eventPublisher.publishEvent(new CommentCreatedEvent(
                post.getUser().getId(),
                commentDTO.getFarmName(),
                commentDTO.getPostId(),
                post.getUser()));
    }

    /**
     * <h3>댓글 수정</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     *
     * @param commentId   댓글 ID
     * @param commentDTO  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void updateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = ValidateComment(commentDTO, userDetails);
        comment.updateComment(commentDTO.getContent());
    }

    /**
     * <h3>댓글 삭제</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     *
     * @param commentId         댓글 ID
     * @param customUserDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void deleteComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = ValidateComment(commentDTO, userDetails);
        commentRepository.delete(comment);
    }

    /**
     * <h3>댓글 유효성 검사</h3>
     *
     * <p>
     * 댓글 수정 및 삭제 시 비밀번호 확인 및 작성자 확인을 수행한다.
     * </p>
     *
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 유효한 댓글 엔티티
     * @throws CustomException 댓글 비밀번호 불일치 또는 작성자 불일치 시 예외 발생
     */
    private Comment ValidateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = commentRepository.getReferenceById(commentDTO.getId());

        if (commentDTO.getPassword() != null && !Objects.equals(comment.getPassword(), commentDTO.getPassword())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentDTO.getPassword() == null && !comment.getUser().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
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
