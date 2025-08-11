package jaeik.growfarm.domain.admin.application.service;

import jaeik.growfarm.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.growfarm.domain.admin.application.port.in.AdminQueryUseCase;
import jaeik.growfarm.domain.admin.application.port.out.LoadReportPort;
import jaeik.growfarm.domain.admin.application.port.out.SendNotificationPort;
import jaeik.growfarm.domain.admin.application.service.resolver.ReportedUserResolver;
import jaeik.growfarm.domain.admin.domain.Report;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService implements AdminCommandUseCase, AdminQueryUseCase {

    private final LoadReportPort loadReportPort;
    private final SendNotificationPort sendNotificationPort;
    private final List<ReportedUserResolver> userResolvers;

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return loadReportPort.findReportsWithPaging(reportType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDTO getReportDetail(Long reportId) {
        Report report = loadReportPort.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));
        return ReportDTO.from(report);
    }

    @Override
    public void banUser(ReportDTO reportDTO) {
        if (reportDTO.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        User user = resolveUser(reportDTO.getReportType(), reportDTO.getTargetId());
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        sendNotificationPort.publishEvent(new UserBannedEvent(this, user.getId(), user.getSocialId(), user.getProvider()));
    }

    private User resolveUser(ReportType reportType, Long targetId) {
        ReportedUserResolver resolver = userResolvers.stream()
                .filter(r -> r.supports().equals(reportType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
        return resolver.resolve(targetId);
    }
}
