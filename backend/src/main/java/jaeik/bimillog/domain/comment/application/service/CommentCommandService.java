package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import jaeik.bimillog.infrastructure.adapter.in.comment.web.CommentCommandController;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>댓글 명령 서비스</h2>
 * <p>댓글 명령 유스케이스의 구현체입니다.</p>
 * <p>댓글 작성, 수정, 삭제, 추천 비즈니스 로직 처리</p>
 * <p>계층형 댓글 구조(Closure Table) 관리</p>
 * <p>익명/회원 댓글 시스템 권한 검증</p>
 * <p>이벤트 발행을 통한 알림 시스템 연동</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentCommandService implements CommentCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final CommentToPostPort commentToPostPort;
    private final GlobalUserQueryPort globalUserQueryPort;
    private final CommentSavePort commentSavePort;
    private final CommentDeletePort commentDeletePort;
    private final CommentQueryPort commentQueryPort;
    private final CommentLikePort commentLikePort;


    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성하고 계층 구조에 맞게 저장합니다.</p>
     * <p>익명/로그인 사용자 구분 처리, Closure Table로 대댓글 계층 구조 생성</p>
     * <p>댓글 작성 완료 후 CommentCreatedEvent 발행으로 알림 시스템 연동</p>
     * <p>{@link CommentCommandController}에서 댓글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param userId 로그인한 사용자 ID (null이면 익명 댓글로 처리)
     * @param postId 게시글 ID
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @param content 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void writeComment(Long userId, Long postId, Long parentId, String content, Integer password) {
        try {
            Post post = commentToPostPort.findById(postId);

            User user = userId != null ? globalUserQueryPort.findById(userId).orElse(null) : null;
            String userName = user != null ? user.getUserName() : "익명";

            saveCommentWithClosure(post, user, content, password, parentId);

            if (post.getUser() != null) {
                eventPublisher.publishEvent(new CommentCreatedEvent(
                        post.getUser().getId(),
                        userName,
                        postId));
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("댓글 작성 중 예상치 못한 오류 발생", e);
            throw new CommentCustomException(CommentErrorCode.COMMENT_WRITE_FAILED, e);
        }
    }

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글의 내용을 수정합니다.</p>
     * <p>익명 댓글: 비밀번호 검증, 회원 댓글: 소유자 검증</p>
     * <p>{@link CommentCommandController}에서 댓글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param commentId 수정할 댓글 ID
     * @param userId 로그인한 사용자 ID (null이면 익명 댓글 권한으로 검증)
     * @param content 새로운 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void updateComment(Long commentId, Long userId, String content, Integer password) {
        Comment comment = validateComment(commentId, userId, password);
        comment.updateComment(content);
    }

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제하며, 자식 댓글 존재 여부에 따라 소프트 삭제 또는 하드 삭제를 수행합니다.</p>
     * <p>자손이 있으면 엔티티 메서드로 소프트 삭제, 없으면 Port를 통한 하드 삭제 처리</p>
     * <p>{@link CommentCommandController}에서 댓글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId, Integer password) {
        Comment comment = validateComment(commentId, userId, password);
        
        if (commentQueryPort.hasDescendants(commentId)) {
            comment.softDelete(); // 더티 체킹으로 자동 업데이트
        } else {
            commentDeletePort.deleteComment(commentId); // 하드 삭제는 Port 사용
        }
    }

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 대한 추천을 토글 방식으로 처리합니다.</p>
     * <p>이미 추천한 댓글을 다시 누르면 취소, 추천하지 않은 댓글을 누르면 추천됩니다.</p>
     * <p>{@link CommentCommandController}에서 댓글 추천 API 처리 시 호출됩니다.</p>
     *
     * @param userId    사용자 ID (로그인한 경우), null인 경우 예외 발생
     * @param commentId 추천/취소할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentQueryPort.findById(commentId);
        User user = globalUserQueryPort.findById(userId)
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
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글을 비즈니스 규칙에 따라 처리합니다.</p>
     * <p>자손이 있는 댓글: 엔티티 메서드로 익명화 (더티 체킹)</p>
     * <p>자손이 없는 댓글: Port를 통한 하드 삭제</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void processUserCommentsOnWithdrawal(Long userId) {
        List<Comment> userComments = commentQueryPort.findAllByUserId(userId);
        
        for (Comment comment : userComments) {
            if (commentQueryPort.hasDescendants(comment.getId())) {
                comment.anonymize(); // 더티 체킹으로 자동 업데이트
            } else {
                commentDeletePort.deleteComment(comment.getId()); // 하드 삭제는 Port 사용
            }
        }
    }

    /**
     * <h3>댓글 권한 검증</h3>
     * <p>댓글 ID와 사용자 정보로 권한을 검증하고 댓글 엔티티를 반환합니다.</p>
     * <p>Comment 엔티티의 canModify 메서드를 활용한 권한 검증</p>
     * <p>updateComment, deleteComment 메서드에서 공통 권한 검증용으로 사용됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(Long commentId, Long userId, Integer password) {
        Comment comment = commentQueryPort.findById(commentId);

        if (!comment.canModify(userId, password)) {
            throw new CommentCustomException(CommentErrorCode.COMMENT_UNAUTHORIZED);
        }
        
        return comment;
    }

    /**
     * <h3>댓글과 클로저 관계 저장</h3>
     * <p>새 댓글을 저장하고 계층 구조 관리를 위한 클로저 관계를 함께 저장합니다.</p>
     * <p>부모 댓글이 있는 경우 상위 클로저 관계를 복사하여 계층 구조 유지</p>
     * <p>writeComment 메서드에서 호출되어 댓글과 클로저 관계를 원자적으로 생성합니다.</p>
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
    }

}
