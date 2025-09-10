package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.admin.in.web.AdminCommandController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>관리자 명령 서비스</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>신고 접수, 사용자 제재, 강제 탈퇴</p>
 * <p>이벤트 기반 도메인 간 통신으로 UserBannedEvent, AdminWithdrawEvent 발행</p>
 * <p>Post/Comment 도메인과 협력하여 신고 대상 유효성 검증</p>
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
     * <h3>신고 및 건의사항 접수 처리</h3>
     * <p>사용자와 관리자의 신고 요청을 처리합니다.</p>
     * <p>익명/로그인 사용자 구분 처리, POST/COMMENT 대상 유효성 검증</p>
     * <p>{@link AdminCommandController}에서 신고 접수 시 호출됩니다.</p>
     *
     * @param userId 신고자 사용자 ID (null이면 익명 신고로 처리)
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 신고 시 필수, ERROR/IMPROVEMENT 시 null 허용)
     * @param content 신고 내용 및 상세 설명
     * @throws AdminCustomException 필수 파라미터 누락이나 잘못된 신고 대상인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void createReport(Long userId, ReportType reportType, Long targetId, String content) {
        validateCreateReportParameters(reportType, targetId);

        User reporter = Optional.ofNullable(userId)
                .flatMap(userQueryPort::findById)
                .orElse(null);

        if (targetId == null && (reportType == ReportType.POST || reportType == ReportType.COMMENT)) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        Report report = Report.createReport(reportType, targetId, content, reporter);
        adminCommandPort.save(report);
    }

    /**
     * <h3>사용자 제재 처리</h3>
     * <p>관리자의 제재 결정을 실행합니다.</p>
     * <p>POST/COMMENT 작성자 조회 후 UserBannedEvent 발행으로 실제 제재 처리</p>
     * <p>{@link AdminCommandController}에서 관리자 제재 결정 시 호출됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용, ERROR/IMPROVEMENT는 예외 발생)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @throws AdminCustomException 잘못된 신고 대상이거나 대상 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void banUser(ReportType reportType, Long targetId) {
        User user = resolveUser(reportType, targetId);
        eventPublisher.publishEvent(new UserBannedEvent(user.getId(), user.getSocialId(), user.getProvider()));
    }

    /**
     * <h3>사용자 강제 탈퇴 처리</h3>
     * <p>관리자의 최종 제재 결정을 실행합니다.</p>
     * <p>POST/COMMENT 작성자 조회 후 AdminWithdrawEvent 발행으로 탈퇴 및 데이터 정리 처리</p>
     * <p>{@link AdminCommandController}에서 관리자 강제 탈퇴 결정 시 호출됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용, ERROR/IMPROVEMENT는 예외 발생)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @throws AdminCustomException 잘못된 신고 대상이거나 대상 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceWithdrawUser(ReportType reportType, Long targetId) {
        User user = resolveUser(reportType, targetId);
        eventPublisher.publishEvent(new AdminWithdrawEvent(user.getId(), "관리자 강제 탈퇴"));
    }

    /**
     * <h3>신고 생성 파라미터 검증</h3>
     * <p>신고 생성 시 필요한 파라미터들의 유효성을 사전 검증합니다.</p>
     * <p>POST/COMMENT 신고는 targetId 필수, ERROR/IMPROVEMENT는 null 허용</p>
     * <p>조기 검증으로 불필요한 데이터베이스 조회 방지</p>
     * <p>createReport에서 파라미터 검증 단계에서 사용됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT, ERROR, IMPROVEMENT)
     * @param targetId 신고 대상 ID (POST/COMMENT 시 필수, ERROR/IMPROVEMENT 시 null 허용)
     * @throws AdminCustomException POST/COMMENT 신고인데 targetId가 null인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateCreateReportParameters(ReportType reportType, Long targetId) {
        if (targetId == null && (reportType == ReportType.POST || reportType == ReportType.COMMENT)) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }
    }

    /**
     * <h3>신고 대상 사용자 조회</h3>
     * <p>제재할 사용자를 식별하고 조회합니다.</p>
     * <p>POST 신고: Post 도메인에서 게시글 작성자 조회, COMMENT 신고: Comment 도메인에서 댓글 작성자 조회</p>
     * <p>ERROR/IMPROVEMENT 신고는 사용자 대상이 아니므로 예외 발생</p>
     * <p>banUser, forceWithdrawUser에서 사용자 식별 단계에서 사용됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @return User 신고 대상 사용자 엔티티
     * @throws AdminCustomException ERROR/IMPROVEMENT 신고이거나 대상 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolveUser(ReportType reportType, Long targetId) {
        if (reportType == ReportType.ERROR || reportType == ReportType.IMPROVEMENT) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }

        User user = switch (reportType) {
            case POST -> postQueryUseCase.findById(targetId).getUser();
            case COMMENT -> commentQueryUseCase.findById(targetId).getUser();
            default -> null;
        };

        if (user == null) {
            throw new AdminCustomException(AdminErrorCode.USER_NOT_FOUND);
        }

        return user;

        // IMPROVEMENT와 ERROR는 도달 불가로 null이 일어나지 않음
        // 글이나 댓글이 있어도 비로그인자나 탈퇴한 회원의 경우 유저가 null일 수 있음.
    }
}
