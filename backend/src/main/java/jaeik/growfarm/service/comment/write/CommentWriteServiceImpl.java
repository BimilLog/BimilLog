package jaeik.growfarm.service.comment.write;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentClosure;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>CommentWriteService</h2>
 * <p>댓글 작성 관련 서비스를 담당하는 클래스입니다.</p>
 *
 * @author jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentWriteServiceImpl implements CommentWriteService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentRepository commentRepository;
    private final CommentClosureRepository commentClosureRepository;

    /**
     * <h3>댓글 작성</h3>
     *
     * <p>
     * 댓글을 DB에 저장하고 글 작성자에게 실시간 알림과 푸시 메시지를 발송한다.
     * </p>
     * <p>
     * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.
     * </p>
     * <p>
     * 클로저 테이블 패턴을 활용하여 계층형태로 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param commentDTO  댓글 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDTO) {
        Post post = postRepository.getReferenceById(commentDTO.getPostId());
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;

        saveCommentWithClosure(
                post,
                user,
                commentDTO.getContent(),
                commentDTO.getPassword(),
                commentDTO.getParentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    post.getUser().getId(),
                    commentDTO.getUserName(),
                    commentDTO.getPostId()));
        }
    }

    /**
     * <h3>댓글 작성 및 클로저 테이블 저장</h3>
     * <p>댓글과 댓글 클로저 테이블에 댓글을 저장한다</p>
     *
     * @param post     댓글이 달릴 게시글
     * @param user     댓글 작성자 (비로그인 시 null)
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택적)
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 1.0.0
     */
    private void saveCommentWithClosure(Post post, Users user, String content, Integer password, Long parentId) {
        try {
            Comment comment = commentRepository.save(Comment.createComment(post, user, content, password));

            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureRepository.save(selfClosure);

            if (parentId != null) {
                Comment parentComment = commentRepository.getReferenceById(parentId);
                List<CommentClosure> parentClosures = commentClosureRepository.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    commentClosureRepository.save(newClosure);
                }
            }

        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }
}
