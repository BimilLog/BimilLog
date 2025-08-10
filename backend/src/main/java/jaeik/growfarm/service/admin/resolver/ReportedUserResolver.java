package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;

public interface ReportedUserResolver {
    Users resolve(Long targetId);
    ReportType supports();
}
