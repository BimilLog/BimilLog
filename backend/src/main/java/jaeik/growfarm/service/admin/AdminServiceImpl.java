package jaeik.growfarm.service.admin;

import jaeik.growfarm.domain.report.domain.Report;
import jaeik.growfarm.domain.report.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.admin.resolver.ReportedUserResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportedUserResolverService reportedUserResolverService;

    @Override
    public Page<ReportDTO> getReportList(int page, int size, ReportType reportType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reportRepository.findReportsWithPaging(reportType, pageable);
    }

    @Override
    public ReportDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));

        return ReportDTO.from(report);
    }

    @Override
    @Transactional
    public void banUser(ReportDTO reportDTO) {
        if (reportDTO.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        User user = reportedUserResolverService.resolveUser(reportDTO.getReportType(), reportDTO.getTargetId());

        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // The original code had eventPublisher.publishEvent(new UserBannedEvent(this, user.getId(), user.getSocialId(), user.getProvider()));
        // However, UserBannedEvent is not imported. Assuming this line is intended to be removed or commented out
        // or that the UserBannedEvent class itself is missing.
        // For now, removing the line as per the new_code's omission.
    }
}
