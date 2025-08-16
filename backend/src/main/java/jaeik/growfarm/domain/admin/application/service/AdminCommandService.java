
package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.growfarm.domain.admin.application.port.in.ReportedUserResolver;
import jaeik.growfarm.domain.admin.application.port.out.AdminCommandPort;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.adapter.admin.in.web.dto.ReportDTO;
import jaeik.growfarm.domain.admin.event.UserBannedEvent;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final List<ReportedUserResolver> userResolvers;
    private final AdminCommandPort adminCommandPort;

    /**
     * <h3>사용자 제재</h3>
     * <p>주어진 신고 정보를 기반으로 사용자를 제재 처리하고 사용자 제재 이벤트를 발행합니다.</p>
     *
     * @param reportDTO 신고 정보 DTO
     * @throws CustomException 잘못된 신고 대상이거나 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void banUser(ReportDTO reportDTO) {
        if (reportDTO.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        User user = resolveUser(reportDTO.getReportType(), reportDTO.getTargetId());
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        eventPublisher.publishEvent(new UserBannedEvent(this, user.getId(), user.getSocialId(), user.getProvider()));
    }

    /**
     * <h3>사용자 강제 탈퇴</h3>
     * <p>주어진 사용자 ID에 해당하는 사용자를 관리자 권한으로 강제 탈퇴 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void forceWithdrawUser(Long userId) {
        adminCommandPort.forceWithdraw(userId);
    }

    /**
     * <h3>신고 대상 사용자 해결</h3>
     * <p>신고 유형과 대상 ID를 기반으로 해당 사용자를 찾아 반환합니다.</p>
     *
     * @param reportType 신고 유형
     * @param targetId   신고 대상 ID
     * @return User 신고 대상 사용자 엔티티
     * @throws CustomException 지원하지 않는 신고 유형이거나 사용자를 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private User resolveUser(ReportType reportType, Long targetId) {
        ReportedUserResolver resolver = userResolvers.stream()
                .filter(r -> r.supports().equals(reportType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
        return resolver.resolve(targetId);
    }
}
