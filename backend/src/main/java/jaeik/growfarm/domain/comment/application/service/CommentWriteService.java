package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentWriteUseCase;
import jaeik.growfarm.domain.comment.application.port.out.*;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.comment.entity.CommentRequest;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentWriteService implements CommentWriteUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final LoadPostPort loadPostPort;
    private final LoadUserPort loadUserPort;
    private final CommentCommandPort commentCommandPort;
    private final CommentQueryPort commentQueryPort;
    private final CommentClosureCommandPort commentClosureCommandPort;
    private final CommentClosureQueryPort commentClosureQueryPort;


    @Override
    public void writeComment(Long userId, CommentRequest commentRequest) {
        Post post = loadPostPort.findById(commentRequest.postId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        User user = null;
        String userName = "익명";
        if (userId != null) {
            user = loadUserPort.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            userName = user.getUserName();
        }

        saveCommentWithClosure(
                post,
                user,
                commentRequest.content(),
                commentRequest.password(),
                commentRequest.parentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    this,
                    post.getUser().getId(),
                    userName,
                    commentRequest.postId()));
        }
    }

    /**
     * <h3>댓글과 클로저 엔티티 함께 저장 (배치 최적화)</h3>
     * <p>새로운 댓글을 저장하고 댓글의 계층 구조를 관리하는 클로저 엔티티를 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 해당 댓글의 모든 상위 클로저 엔티티와 새로운 댓글을 연결합니다.</p>
     * <p>성능 최적화: 클로저 엔티티들을 배치 저장으로 처리하여 N번의 INSERT를 1번으로 최적화합니다.</p>
     *
     * @param post     댓글이 속한 게시글 엔티티
     * @param user     댓글 작성 사용자 엔티티
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택 사항)
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    private void saveCommentWithClosure(Post post, User user, String content, Integer password, Long parentId) {
        try {
            Comment comment = commentCommandPort.save(Comment.createComment(post, user, content, password));

            // 클로저 엔티티들을 배치로 저장하기 위한 리스트
            List<CommentClosure> closuresToSave = new ArrayList<>();
            
            // 자기 자신에 대한 클로저 추가 (depth = 0)
            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            closuresToSave.add(selfClosure);

            // 부모 댓글이 있는 경우 부모의 모든 조상과의 클로저 생성
            if (parentId != null) {
                Comment parentComment = commentQueryPort.findById(parentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
                List<CommentClosure> parentClosures = commentClosureQueryPort.findByDescendantId(parentComment.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.PARENT_COMMENT_NOT_FOUND));

                for (CommentClosure parentClosure : parentClosures) {
                    Comment ancestor = parentClosure.getAncestor();
                    int newDepth = parentClosure.getDepth() + 1;
                    CommentClosure newClosure = CommentClosure.createCommentClosure(ancestor, comment, newDepth);
                    closuresToSave.add(newClosure);
                }
            }

            // 모든 클로저 엔티티를 한 번에 배치 저장
            commentClosureCommandPort.saveAll(closuresToSave);

        } catch (CustomException e) {
            // 비즈니스 예외는 그대로 재발행
            throw e;
        } catch (Exception e) {
            // 기술적 오류만 COMMENT_WRITE_FAILED로 감싸기
            throw new CustomException(ErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }
}
