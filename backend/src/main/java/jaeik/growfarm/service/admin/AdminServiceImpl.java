package jaeik.growfarm.service.admin;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.event.UserBannedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.service.admin.resolver.ReportedUserResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final ReportedUserResolverService resolverService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportRepository.findReportsWithPaging(reportType, pageable);
    }

    @Override
    public ReportDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));

        return ReportDTO.createReportDTO(report);
    }

    @Override
    @Transactional
    public void banUser(ReportDTO reportDTO) {
        if (reportDTO.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        Users user = resolverService.resolveUser(reportDTO.getReportType(), reportDTO.getTargetId());

        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        eventPublisher.publishEvent(new UserBannedEvent(this, user.getId(), user.getSocialId(), user.getProvider()));
    }
}
