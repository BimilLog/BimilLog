package jaeik.bimillog.domain.admin.application.service;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.admin.application.port.out.AdminCommandPort;
import jaeik.bimillog.domain.admin.entity.Report;
import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.admin.event.UserForcedWithdrawalEvent;
import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.admin.web.AdminCommandController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>관리자 명령 서비스</h2>
 * <p>관리자 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>신고 접수, 사용자 제재, 강제 탈퇴</p>
 * <p>이벤트 기반 도메인 간 통신으로 UserBannedEvent, UserForcedWithdrawalEvent 발행</p>
 * <p>Post/Comment 도메인과 협력하여 신고 대상 유효성 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class AdminCommandService implements AdminCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final AdminCommandPort adminCommandPort;
    private final UserQueryPort userQueryPort;
    private final PostQueryUseCase postQueryUseCase; // 어댑터에서 의존으로 해야함
    private final CommentQueryUseCase commentQueryUseCase; // 어댑터에서 의존으로 해야함
    private final UserCommandUseCase userCommandUseCase; // 어댑터에서 의존으로 해야함
    private final UserBanUseCase userBanUseCase; // 어댑터에서 의존으로 해야함

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
    @Transactional
    public void createReport(Long userId, ReportType reportType, Long targetId, String content) {
        User reporter = Optional.ofNullable(userId)
                .flatMap(userQueryPort::findById)
                .orElse(null);

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
    @Transactional
    public void banUser(ReportType reportType, Long targetId) {
        User user = resolveUser(reportType, targetId);
        userCommandUseCase.banUser(user.getId());
        userBanUseCase.addToBlacklist(user.getId(), user.getSocialId(), user.getProvider());
        userBanUseCase.blacklistAllUserTokens(user.getId());
        eventPublisher.publishEvent(new UserBannedEvent(user.getId(), user.getSocialId(), user.getProvider(), "사용자 제재"));
    }

    /**
     * <h3>사용자 강제 탈퇴 처리</h3>
     * <p>관리자의 최종 제재 결정을 실행합니다.</p>
     * <p>POST/COMMENT 작성자 조회 후 UserForcedWithdrawalEvent 발행으로 탈퇴 및 데이터 정리 처리</p>
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
        userBanUseCase.addToBlacklist(user.getId(), user.getSocialId(), user.getProvider());
        userBanUseCase.blacklistAllUserTokens(user.getId());
        eventPublisher.publishEvent(new UserForcedWithdrawalEvent(user.getId(), user.getSocialId(), user.getProvider(), "사용자 강제탈퇴"));
    }

    @Override
    public void deleteAllReportsByUserId(Long userId) {

    }

    /**
     * <h3>신고 대상 사용자 조회</h3>
     * <p>제재할 사용자를 식별하고 조회합니다.</p>
     * <p>POST 신고: Post 도메인에서 게시글 작성자 조회, COMMENT 신고: Comment 도메인에서 댓글 작성자 조회</p>
     * <p>banUser, forceWithdrawUser에서 사용자 식별 단계에서 사용됩니다.</p>
     *
     * @param reportType 신고 유형 (POST, COMMENT만 허용)
     * @param targetId 신고 대상 ID (게시글 ID 또는 댓글 ID)
     * @return User 신고 대상 사용자 엔티티
     * @throws AdminCustomException 대상 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolveUser(ReportType reportType, Long targetId) {
        User user = switch (reportType) {
            case POST -> postQueryUseCase.findById(targetId).getUser();
            case COMMENT -> commentQueryUseCase.findById(targetId).getUser();
            default -> throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        };

        if (user == null) {
            throw new AdminCustomException(AdminErrorCode.USER_NOT_FOUND);
        }

        return user;
    }
}
