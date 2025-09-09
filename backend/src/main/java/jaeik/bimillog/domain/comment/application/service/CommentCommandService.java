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
 * <h2>CommentCommandService</h2>
 * <p>
 * 댓글 명령 관련 UseCase 인터페이스의 구체적 구현체로서 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 댓글 도메인의 명령 처리를 담당하며, 계층형 댓글 구조(Closure Table)와 
 * 익명/회원 댓글 시스템의 복잡한 비즈니스 규칙을 관리합니다.
 * </p>
 * <p>
 * 트랜잭션 경계를 설정하고 이벤트 기반 도메인 간 통신을 통해 알림 발송 등 부수 효과를 처리합니다.
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
     * <h3>댓글 작성 비즈니스 로직 실행</h3>
     * <p>CommentCommandUseCase 인터페이스의 댓글 작성 기능을 구현하며, 계층형 댓글 시스템의 핵심 비즈니스 규칙을 적용합니다.</p>
     * <p>익명 사용자와 로그인 사용자를 구분하여 처리하고, Closure Table 패턴으로 대댓글 계층 구조를 생성합니다.</p>
     * <p>트랜잭션 내에서 댓글과 클로저 엔티티를 원자적으로 저장하고, 게시글 작성자에게 알림을 위한 이벤트를 발행합니다.</p>
     * <p>CommentCreatedEvent를 통해 notification 도메인과 비동기 통신하여 실시간 알림을 제공합니다.</p>
     *
     * @param userId         로그인한 사용자 ID (null이면 익명 댓글로 처리)
     * @param commentRequest 댓글 생성 요청 정보 (내용, 게시글 ID, 부모 댓글 ID, 비밀번호 포함)
     * @throws CustomException 게시글이 존재하지 않거나 부모 댓글을 찾을 수 없는 경우
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
     * <h3>댓글 수정 권한 검증 및 업데이트</h3>
     * <p>CommentCommandUseCase 인터페이스의 댓글 수정 기능을 구현하며, 익명/회원 댓글의 서로 다른 권한 검증 로직을 적용합니다.</p>
     * <p>익명 댓글의 경우 비밀번호 일치 여부를, 회원 댓글의 경우 소유자 일치 여부를 검증합니다.</p>
     * <p>트랜잭션 내에서 댓글 엔티티의 내용만 업데이트하며, 계층 구조나 메타데이터는 변경하지 않습니다.</p>
     * <p>권한 검증 실패 시 적절한 도메인 예외를 발생시켜 보안을 보장합니다.</p>
     *
     * @param userId         로그인한 사용자 ID (null이면 익명 댓글 권한으로 검증)
     * @param commentRequest 댓글 수정 요청 정보 (댓글 ID, 새 내용, 비밀번호 포함)
     * @throws CustomException 댓글을 찾을 수 없거나 권한 검증에 실패한 경우
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
