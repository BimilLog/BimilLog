package jaeik.growfarm.service.comment.like;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>CommentLikeService</h2>
 * <p>댓글 추천 관련 서비스를 담당하는 클래스입니다.</p>
 *
 * @author jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

    /**
     * <h3>댓글 추천</h3>
     *
     * <p>
     * 댓글을 추천하거나 추천 취소한다.
     * </p>
     * <p>
     * 이미 추천한 경우 추천을 취소하고, 추천하지 않은 경우 추천을 추가한다.
     * </p>
     *
     * @param commentDTO  추천할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public void likeComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Long commentId = commentDTO.getId();
        Long userId = userDetails.getUserId();

        Comment comment = commentRepository.getReferenceById(commentId);
        Users user = userRepository.getReferenceById(userId);

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

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
}
