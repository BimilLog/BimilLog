
package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 명령 서비스</h2>
 * <p>관리자 명령 유스케이스(`AdminCommandUseCase`)를 구현하는 서비스 클래스</p>
 * <p>사용자 제재 및 강제 탈퇴와 같은 관리자 기능을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminCommandService implements AdminCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final AdminCommandPort adminCommandPort;
    private final UserQueryPort userQueryPort;
    private final PostQueryUseCase postQueryUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * {@inheritDoc}
     *
     * <p>사용자가 제출한 신고나 건의사항을 생성하여 저장합니다.</p>
     * <p>인증된 사용자의 경우 사용자 정보를 조회하고, 익명 사용자의 경우 null로 처리합니다.</p>
     * <p>신고 유형에 따른 비즈니스 검증을 수행합니다.</p>
     */
    @Override
    public void createReport(Long userId, ReportType reportType, Long targetId, String content) {
        // 신고자 정보 조회 (익명 사용자의 경우 null)
        User reporter = null;
        if (userId != null) {
            reporter = userQueryPort.findById(userId)
                    .orElseThrow(() -> new AdminCustomException(AdminErrorCode.USER_NOT_FOUND));
        }
        
        // 신고 대상 존재 여부 검증 (POST, COMMENT만)
        if ((reportType == ReportType.POST || reportType == ReportType.COMMENT) && targetId != null) {
            validateTargetExists(reportType, targetId);
        }
        
        // Report 엔티티 생성 및 저장 (내부에서 도메인 검증 수행)
        Report report = Report.createReport(reportType, targetId, content, reporter);
        adminCommandPort.save(report);
    }

    /**
     * <h3>사용자 제재</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 제재 처리하고 사용자 제재 이벤트를 발행합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @throws CustomException 잘못된 신고 대상이거나 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void banUser(ReportType reportType, Long targetId) {
        // POST, COMMENT 타입은 targetId가 필수
        if ((reportType == ReportType.POST || reportType == ReportType.COMMENT) 
                && targetId == null) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        // ERROR, IMPROVEMENT 타입은 banUser 대상이 없음
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        User user = resolveUser(reportType, targetId);
        if (user == null) {
            throw new AdminCustomException(AdminErrorCode.USER_NOT_FOUND);
        }

        eventPublisher.publishEvent(new UserBannedEvent(user.getId(), user.getSocialId(), user.getProvider()));
    }

    /**
     * <h3>사용자 강제 탈퇴</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 관리자 권한으로 강제 탈퇴 처리합니다.</p>
     * <p>이벤트 드리븐 방식으로 Auth 도메인에 탈퇴 요청을 전달합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceWithdrawUser(ReportType reportType, Long targetId) {
        // POST, COMMENT 타입은 targetId가 필수
        if ((reportType == ReportType.POST || reportType == ReportType.COMMENT) 
                && targetId == null) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        // ERROR, IMPROVEMENT 타입은 forceWithdrawUser 대상이 없음
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        User user = resolveUser(reportType, targetId);
        if (user == null) {
            throw new AdminCustomException(AdminErrorCode.USER_NOT_FOUND);
        }
        
        // 이벤트 발행으로 Auth 도메인에 탈퇴 처리 위임
        eventPublisher.publishEvent(new AdminWithdrawRequestedEvent(user.getId(), "관리자 강제 탈퇴"));
    }

    /**
     * <h3>신고 대상 존재 여부 검증</h3>
     * <p>신고 대상이 실제로 존재하는지 검증합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @throws CustomException 대상이 존재하지 않거나 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateTargetExists(ReportType reportType, Long targetId) {
        try {
            User targetUser = resolveUser(reportType, targetId);
            if (targetUser == null) {
                throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
            }
        } catch (CustomException e) {
            // 대상을 찾을 수 없는 경우 신고 대상이 유효하지 않음으로 처리
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }
    }

    /**
     * <h3>신고 대상 사용자 해결</h3>
     * <p>신고 유형과 대상 ID를 기반으로 해당 사용자를 찾아 반환합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @return User 신고 대상 사용자 엔티티, ERROR/IMPROVEMENT 타입의 경우 null
     * @throws CustomException 지원하지 않는 신고 유형이거나 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolveUser(ReportType reportType, Long targetId) {
        return switch (reportType) {
            case POST -> resolvePostUser(targetId);
            case COMMENT -> resolveCommentUser(targetId);
            case ERROR, IMPROVEMENT -> null; // 타겟 없는 타입
        };
    }

    /**
     * <h3>게시글 ID로 신고 대상 사용자 해결</h3>
     * <p>주어진 게시글 ID에 해당하는 게시글의 작성자(사용자)를 조회하여 반환합니다.</p>
     *
     * @param postId 게시글 ID
     * @return User 게시글 작성 사용자 엔티티
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolvePostUser(Long postId) {
        Post post = postQueryUseCase.findById(postId)
                .orElseThrow(() -> new AdminCustomException(AdminErrorCode.POST_NOT_FOUND));
        return post.getUser();
    }

    /**
     * <h3>댓글 ID로 신고 대상 사용자 해결</h3>
     * <p>주어진 댓글 ID에 해당하는 댓글의 작성자(사용자)를 조회하여 반환합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return User 댓글 작성 사용자 엔티티
     * @throws CustomException 댓글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolveCommentUser(Long commentId) {
        Comment comment = commentQueryUseCase.findById(commentId)
                .orElseThrow(() -> new CommentCustomException(CommentErrorCode.COMMENT_FAILED));
        return comment.getUser();
    }
}
