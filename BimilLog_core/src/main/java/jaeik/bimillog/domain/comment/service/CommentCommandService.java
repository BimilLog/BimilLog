package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.out.CommentDeleteAdapter;
import jaeik.bimillog.domain.comment.out.CommentLikeAdapter;
import jaeik.bimillog.domain.comment.out.CommentQueryAdapter;
import jaeik.bimillog.domain.comment.out.CommentSaveAdapter;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.comment.event.CommentDeletedEvent;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCommentQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalMemberQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.comment.in.web.CommentCommandController;
import jaeik.bimillog.domain.post.out.PostToCommentAdapter;
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
public class CommentCommandService {

    private final ApplicationEventPublisher eventPublisher;
    private final GlobalPostQueryPort globalPostQueryPort;
    private final GlobalMemberQueryPort globalUserQueryPort;
    private final GlobalCommentQueryPort globalCommentQueryPort;
    private final CommentSaveAdapter commentSaveAdapter;
    private final CommentDeleteAdapter commentDeleteAdapter;
    private final CommentQueryAdapter commentQueryAdapter;
    private final CommentLikeAdapter commentLikeAdapter;


    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성하고 계층 구조에 맞게 저장합니다.</p>
     * <p>익명/로그인 사용자 구분 처리, Closure Table로 대댓글 계층 구조 생성</p>
     * <p>댓글 작성 완료 후 CommentCreatedEvent 발행으로 알림 시스템 연동</p>
     * <p>{@link CommentCommandController}에서 댓글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param memberId   로그인한 사용자 ID (null이면 익명 댓글로 처리)
     * @param postId   게시글 ID
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void writeComment(Long memberId, Long postId, Long parentId, String content, Integer password) {
        try {
            Post post = globalPostQueryPort.findById(postId);

            Member member = memberId != null ? globalUserQueryPort.findById(memberId).orElse(null) : null;
            String memberName = member != null ? member.getMemberName() : "익명";

            saveCommentWithClosure(post, member, content, password, parentId);

            if (post.getMember() != null) {
                eventPublisher.publishEvent(new CommentCreatedEvent(
                        post.getMember().getId(),
                        memberName,
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
     * @param memberId    로그인한 사용자 ID (null이면 익명 댓글 권한으로 검증)
     * @param content   새로운 댓글 내용
     * @param password  댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void updateComment(Long commentId, Long memberId, String content, Integer password) {
        Comment comment = validateComment(commentId, memberId, password);
        comment.updateComment(content);
    }

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제하며, 자식 댓글 존재 여부에 따라 소프트 삭제 또는 하드 삭제를 수행합니다.</p>
     * <p>자손이 있으면 엔티티 메서드로 소프트 삭제, 없으면 Port를 통한 하드 삭제 처리</p>
     * <p>{@link CommentCommandController}에서 댓글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @param memberId    사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password  댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void deleteComment(Long commentId, Long memberId, Integer password) {
        Comment comment = validateComment(commentId, memberId, password);
        Long postId = comment.getPost().getId();

        if (commentQueryAdapter.hasDescendants(commentId)) {
            comment.softDelete(); // 더티 체킹으로 자동 업데이트
        } else {
            commentDeleteAdapter.deleteComment(commentId); // 어댑터 직접 호출로 하드 삭제
        }

        // 실시간 인기글 점수 감소 이벤트 발행
        eventPublisher.publishEvent(new CommentDeletedEvent(postId));
    }

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 대한 추천을 토글 방식으로 처리합니다.</p>
     * <p>이미 추천한 댓글을 다시 누르면 취소, 추천하지 않은 댓글을 누르면 추천됩니다.</p>
     * <p>{@link CommentCommandController}에서 댓글 추천 API 처리 시 호출됩니다.</p>
     *
     * @param memberId    사용자 ID (로그인한 경우), null인 경우 예외 발생
     * @param commentId 추천/취소할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void likeComment(Long memberId, Long commentId) {
        Comment comment = globalCommentQueryPort.findById(commentId);
        Member member = globalUserQueryPort.findById(memberId)
                .orElseThrow(() -> new MemberCustomException(MemberErrorCode.USER_NOT_FOUND));

        if (commentLikeAdapter.isLikedByUser(commentId, memberId)) {
            commentLikeAdapter.deleteLikeByIds(commentId, memberId);
        } else {
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .member(member)
                    .build();
            commentLikeAdapter.save(commentLike);
        }
    }

    /**
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글을 비즈니스 규칙에 따라 처리합니다.</p>
     * <p>자손이 있는 댓글: 엔티티 메서드로 익명화 (더티 체킹)</p>
     * <p>자손이 없는 댓글: Port를 통한 하드 삭제</p>
     *
     * @param memberId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void processUserCommentsOnWithdrawal(Long memberId) {
        List<Comment> userComments = commentQueryAdapter.findAllByMemberId(memberId);

        for (Comment comment : userComments) {
            if (commentQueryAdapter.hasDescendants(comment.getId())) {
                comment.anonymize(); // 더티 체킹으로 자동 업데이트
            } else {
                commentDeleteAdapter.deleteComment(comment.getId()); // 어댑터 직접 호출로 하드 삭제
            }
        }
    }

    /**
     * <h3>특정 글의 모든 댓글 삭제</h3>
     * <p>게시글 삭제 시 해당 글의 모든 댓글을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>트랜잭션 내에서 실행되어 삭제 중 오류 발생 시 롤백됩니다.</p>
     * <p>{@link PostToCommentAdapter}를 통해 Post 도메인에서 호출됩니다.</p>
     *
     * @param postId 댓글을 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public void deleteCommentsByPost(Long postId) {
        List<Comment> userComments = commentQueryAdapter.findAllByPostId(postId);

        for (Comment comment : userComments) {
            commentDeleteAdapter.deleteComment(comment.getId()); // 어댑터 직접 호출로 하드 삭제
        }
    }

    /**
     * <h3>댓글 권한 검증</h3>
     * <p>댓글 ID와 사용자 정보로 권한을 검증하고 댓글 엔티티를 반환합니다.</p>
     * <p>Comment 엔티티의 canModify 메서드를 활용한 권한 검증</p>
     * <p>updateComment, deleteComment 메서드에서 공통 권한 검증용으로 사용됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @param memberId    사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password  댓글 비밀번호 (익명 댓글인 경우)
     * @return Comment 유효성 검사를 통과한 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    private Comment validateComment(Long commentId, Long memberId, Integer password) {
        Comment comment = globalCommentQueryPort.findById(commentId);

        if (!comment.canModify(memberId, password)) {
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
     * @param member     댓글 작성 사용자 엔티티
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택 사항)
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    private void saveCommentWithClosure(Post post, Member member, String content, Integer password, Long parentId) {
        Comment comment = Comment.createComment(post, member, content, password);
        Comment savedComment = commentSaveAdapter.save(comment);

        List<CommentClosure> closuresToSave = new ArrayList<>();
        closuresToSave.add(CommentClosure.createCommentClosure(savedComment, savedComment, 0));

        if (parentId != null) {
            List<CommentClosure> parentClosures = commentSaveAdapter.getParentClosures(parentId)
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));

            for (CommentClosure parentClosure : parentClosures) {
                closuresToSave.add(CommentClosure.createCommentClosure(
                        parentClosure.getAncestor(),
                        savedComment,
                        parentClosure.getDepth() + 1));
            }
        }
        commentSaveAdapter.saveAll(closuresToSave);
    }

}
