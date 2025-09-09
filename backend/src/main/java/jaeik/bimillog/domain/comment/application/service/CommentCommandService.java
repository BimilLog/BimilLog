package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <h2>댓글 명령 서비스</h2>
 * <p>
 * 댓글 생성, 수정, 삭제 등 명령 관련 기능을 구현하는 서비스 클래스
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 댓글 명령 처리를 담당하는 비즈니스 로직 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final CommentToPostPort commentToPostPort;
    private final CommentToUserPort commentToUserPort;
    private final CommentSavePort commentSavePort;
    private final CommentDeletePort commentDeletePort;
    private final CommentQueryPort commentQueryPort;
    private final CommentLikePort commentLikePort;


    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성합니다.</p>
     * <p>부모 댓글이 있는 경우 대댓글로 처리하고, 댓글 생성 이벤트를 발행합니다.</p>
     *
     * @param userId         사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest 댓글 요청 (비밀번호 포함)
     * @throws CustomException 게시글이나 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void writeComment(Long userId, Comment.Request commentRequest) {
        Post post = commentToPostPort.findById(commentRequest.postId());

        User user = Optional.ofNullable(userId)
                .flatMap(commentToUserPort::findById)
                .orElse(null);

        String userName = (user != null) ? user.getUserName() : "익명";

        saveCommentWithClosure(
                post,
                user,
                commentRequest.content(),
                commentRequest.password(),
                commentRequest.parentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    post.getUser().getId(),
                    userName,
                    commentRequest.postId()));
        }
    }

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글의 내용을 수정합니다.</p>
     * <p>댓글의 권한을 확인한 후 내용을 업데이트합니다.</p>
     *
     * @param userId         사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest 수정할 댓글 요청 (비밀번호 포함)
     * @throws CustomException 댓글이 존재하지 않거나 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updateComment(Long userId, Comment.Request commentRequest) {
        Comment comment = validateComment(commentRequest, userId);
        comment.updateComment(commentRequest.content());
    }

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제합니다. 자손 댓글이 있는 경우 소프트 삭제, 없는 경우 하드 삭제를 수행합니다.</p>
     *
     * @param userId         사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param commentRequest 삭제할 댓글 요청 (비밀번호 포함)
     * @throws CustomException 댓글이 존재하지 않거나 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteComment(Long userId, Comment.Request commentRequest) {
        Comment comment = validateComment(commentRequest, userId);
        handleCommentDeletion(comment.getId());
    }

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>사용자가 댓글에 추천을 누르거나 취소합니다.</p>
     * <p>이미 추천한 댓글이면 취소하고, 추천하지 않은 댓글이면 추천을 추가합니다.</p>
     *
     * @param userId    사용자 ID (로그인한 경우), null인 경우 예외 발생
     * @param commentId 추천/취소할 댓글 ID
     * @throws CustomException 사용자나 댓글이 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentQueryPort.findById(commentId);
        User user = commentToUserPort.findById(userId)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));

        if (commentLikePort.isLikedByUser(commentId, userId)) {
            commentLikePort.deleteLikeByIds(commentId, userId);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikePort.save(commentLike);
        }
    }

    /**
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글에 대해 적절한 처리를 수행합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void processUserCommentsOnWithdrawal(Long userId) {
        commentDeletePort.processUserCommentsOnWithdrawal(userId);
    }

    /**
     * <h3>댓글 유효성 검사 및 조회</h3>
     * <p>댓글 요청과 사용자 ID를 기반으로 댓글의 유효성을 검사하고 댓글 엔티티를 조회합니다.</p>
     * <p>비밀번호가 일치하지 않거나, 사용자 본인이 아닌 경우 예외를 발생시킵니다.</p>
     *
     * @param commentRequest 댓글 요청 (비밀번호 포함)
     * @param userId         사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @throws CustomException 댓글을 찾을 수 없거나, 비밀번호가 일치하지 않거나, 사용자 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(Comment.Request commentRequest, Long userId) {
        Comment comment = commentQueryPort.findById(commentRequest.id());

        // 비밀 댓글인 경우 비밀번호 확인
        if (commentRequest.password() != null) {
            if (!Objects.equals(comment.getPassword(), commentRequest.password())) {
                throw new CommentCustomException(CommentErrorCode.COMMENT_PASSWORD_NOT_MATCH);
            }
        } else {
            // 로그인 사용자이고 소유자인지 확인
            if (userId == null || comment.getUser() == null || !Objects.equals(comment.getUser().getId(), userId)) {
                throw new CommentCustomException(CommentErrorCode.ONLY_COMMENT_OWNER_UPDATE);
            }
        }
        return comment;
    }

    /**
     * <h3>댓글과 클로저 엔티티 함께 저장</h3>
     * <p>새로운 댓글을 저장하고 댓글의 계층 구조를 관리하는 클로저 엔티티를 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 해당 댓글의 모든 상위 클로저 엔티티와 새로운 댓글을 연결합니다.</p>
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
            Comment comment = Comment.createComment(post, user, content, password);
            Comment savedComment = commentSavePort.save(comment);

            List<CommentClosure> closuresToSave = new ArrayList<>();
            closuresToSave.add(CommentClosure.createCommentClosure(savedComment, savedComment, 0));

            if (parentId != null) {
                List<CommentClosure> parentClosures = commentSavePort.getParentClosures(parentId)
                        .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));

                for (CommentClosure parentClosure : parentClosures) {
                    closuresToSave.add(CommentClosure.createCommentClosure(
                            parentClosure.getAncestor(),
                            savedComment,
                            parentClosure.getDepth() + 1));
                }
            }
            commentSavePort.saveAll(closuresToSave);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 작성 중 예상치 못한 오류 발생", e);
            throw new CommentCustomException(CommentErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }

    /**
     * <h3>댓글 삭제 처리 (하드/소프트 삭제)</h3>
     * <p>댓글 ID를 기반으로 자손이 있는지 확인하여 적절한 삭제 방식을 선택합니다.</p>
     * <p>자손이 없으면 하드 삭제를, 있으면 소프트 삭제를 수행합니다.</p>
     *
     * @param commentId 삭제 대상 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    private void handleCommentDeletion(Long commentId) {
        commentDeletePort.deleteComment(commentId);
    }
}
