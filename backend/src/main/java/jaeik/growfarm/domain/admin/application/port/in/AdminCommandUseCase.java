package jaeik.growfarm.domain.admin.application.port.in;

import jaeik.growfarm.dto.admin.ReportDTO;

public interface AdminCommandUseCase {

    void banUser(ReportDTO reportDTO);
    void forceWithdrawUser(Long userId);
}
