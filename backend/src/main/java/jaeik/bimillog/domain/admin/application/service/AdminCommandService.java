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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
     * <h3>신고 / 건의사항 저장</h3>
     *
     * <p>사용자가 제출한 신고나 건의사항을 생성하여 저장합니다.</p>
     * <p>인증된 사용자의 경우 사용자 정보를 조회하고, 익명 사용자의 경우 null로 처리합니다.</p>
     * <p>신고 유형에 따른 비즈니스 검증을 수행합니다.</p>
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
     * <h3>사용자 제재</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 제재 처리하고 사용자 제재 이벤트를 발행합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @throws AdminCustomException 잘못된 신고 대상이거나 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void banUser(ReportType reportType, Long targetId) {
        User user = resolveUser(reportType, targetId);
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
        User user = resolveUser(reportType, targetId);
        eventPublisher.publishEvent(new AdminWithdrawEvent(user.getId(), "관리자 강제 탈퇴"));
    }

    /**
     * <h3>신고 생성 파라미터 검증</h3>
     * <p>신고 생성 시 필요한 파라미터들이 유효한지 검증합니다.</p>
     *
     * @param reportType 신고 유형 (필수)
     * @param targetId   신고 대상 ID
     * @throws AdminCustomException 필수 파라미터가 누락되거나 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateCreateReportParameters(ReportType reportType, Long targetId) {
        if (targetId == null && (reportType == ReportType.POST || reportType == ReportType.COMMENT)) {
            throw new AdminCustomException(AdminErrorCode.INVALID_REPORT_TARGET);
        }
        // 나머지 검증은 DTO에서 검증함
    }

    /**
     * <h3>신고 대상 사용자 조회</h3>
     * <p>신고 유형과 대상 ID를 기반으로 해당 사용자를 찾아 반환합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @return User 신고 대상 사용자 엔티티, ERROR/IMPROVEMENT 타입의 경우 null
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
