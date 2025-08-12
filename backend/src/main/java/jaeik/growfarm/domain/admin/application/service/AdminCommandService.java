
package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.growfarm.domain.admin.application.port.out.UserAuthPort;
import jaeik.growfarm.domain.admin.application.service.resolver.ReportedUserResolver;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminCommandService implements AdminCommandUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final List<ReportedUserResolver> userResolvers;
    private final UserAuthPort userAuthPort;

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

    @Override
    public void forceWithdrawUser(Long userId) {
        userAuthPort.forceWithdraw(userId);
    }

    private User resolveUser(ReportType reportType, Long targetId) {
        ReportedUserResolver resolver = userResolvers.stream()
                .filter(r -> r.supports().equals(reportType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
        return resolver.resolve(targetId);
    }
}
