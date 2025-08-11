package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.domain.report.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;

public interface ReportedUserResolver {
    User resolve(Long targetId);
    ReportType supports();
}
