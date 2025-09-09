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
 * <h2>AdminCommandService</h2>
 * <p>
 * 관리자 명령 관련 UseCase 인터페이스의 구체적 구현체로서 비즈니스 로직을 오케스트레이션합니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 관리자 도메인의 명령 처리를 담당하며, 신고 접수, 사용자 제재, 강제 탈퇴 등
 * 관리자 권한이 필요한 핵심 비즈니스 규칙을 관리합니다.
 * </p>
 * <p>
 * AdminCommandController에서 관리자의 요청을 받아 처리하고, 이벤트 기반 도메인 간 통신을 통해
 * Auth 도메인으로 사용자 제재/탈퇴 처리를 위임합니다.
 * </p>
 * <p>
 * 트랜잭션 경계를 설정하고 Post/Comment 도메인과의 협력을 통해 신고 대상의 유효성을 검증합니다.
 * </p>
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
     * <p>AdminCommandUseCase 인터페이스의 신고 접수 기능을 구현하며, 사용자와 관리자의 신고 요청을 처리합니다.</p>
     * <p>프론트엔드의 신고 폼에서 제출된 데이터를 받아 Report 엔티티로 생성하고 저장소에 저장합니다.</p>
     * <p>익명 사용자와 로그인 사용자를 구분하여 처리하고, POST/COMMENT 신고 시에는 대상 ID 유효성을 검증합니다.</p>
     * <p>AdminCommandController에서 사용자의 신고 요청이나 관리자의 직접 신고 등록 시 호출됩니다.</p>
     * <p>신고 유형(ERROR, IMPROVEMENT)에 따라 서로 다른 검증 로직을 적용하여 데이터 무결성을 보장합니다.</p>
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
     * <p>AdminCommandUseCase 인터페이스의 사용자 제재 기능을 구현하며, 관리자의 제재 결정을 실행합니다.</p>
     * <p>관리자 대시보드에서 신고를 검토한 후 제재가 필요하다고 판단될 때 AdminCommandController에서 호출됩니다.</p>
     * <p>신고 유형과 대상 ID를 통해 해당 게시글이나 댓글의 작성자를 조회하고 UserBannedEvent를 발행합니다.</p>
     * <p>이벤트 기반 아키텍처를 통해 Auth 도메인의 UserBanListener가 실제 사용자 제재 처리를 담당합니다.</p>
     * <p>Post 도메인과 Comment 도메인에 사용자 조회를 위임하여 도메인 간 결합도를 낮춥니다.</p>
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
     * <p>AdminCommandUseCase 인터페이스의 강제 탈퇴 기능을 구현하며, 관리자의 최종 제재 결정을 실행합니다.</p>
     * <p>관리자 대시보드에서 심각한 위반 사례로 판단하여 영구 제재가 필요할 때 AdminCommandController에서 호출됩니다.</p>
     * <p>신고 유형과 대상 ID를 통해 해당 게시글이나 댓글의 작성자를 조회하고 AdminWithdrawEvent를 발행합니다.</p>
     * <p>이벤트 기반 아키텍처를 통해 Auth 도메인의 AdminWithdrawListener가 실제 사용자 탈퇴 처리를 수행합니다.</p>
     * <p>단순 제재와 달리 사용자의 모든 데이터를 정리하고 재가입을 차단하는 강력한 조치입니다.</p>
     * <p>Comment 도메인의 processUserCommentsOnWithdrawal과 연동하여 탈퇴자의 댓글도 함께 처리됩니다.</p>
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
     * <p>createReport 메서드에서 호출되어 신고 생성 시 필요한 파라미터들의 유효성을 사전 검증합니다.</p>
     * <p>POST/COMMENT 신고의 경우 반드시 targetId가 필요하지만, ERROR/IMPROVEMENT는 대상이 없어도 됩니다.</p>
     * <p>비즈니스 규칙에 따른 조기 검증을 통해 불필요한 데이터베이스 조회를 방지하고 성능을 향상시킵니다.</p>
     * <p>DTO 계층에서 기본 검증이 완료된 후 도메인 레벨의 추가 검증을 수행하는 역할입니다.</p>
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
        // 나머지 검증은 DTO에서 검증함
    }

    /**
     * <h3>신고 대상 사용자 조회</h3>
     * <p>banUser와 forceWithdrawUser 메서드에서 호출되어 제재할 사용자를 식별하고 조회합니다.</p>
     * <p>POST 신고의 경우 Post 도메인에 위임하여 게시글 작성자를, COMMENT 신고의 경우 Comment 도메인에 위임하여 댓글 작성자를 조회합니다.</p>
     * <p>도메인 간 결합도를 낮추기 위해 각 도메인의 UseCase를 통해 간접적으로 사용자 정보를 획득합니다.</p>
     * <p>ERROR/IMPROVEMENT 신고는 특정 사용자 대상이 아니므로 예외를 발생시켜 잘못된 사용을 방지합니다.</p>
     * <p>익명 사용자나 탈퇴한 회원의 게시글/댓글인 경우 null을 반환할 수 있어 추가 검증이 필요합니다.</p>
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
