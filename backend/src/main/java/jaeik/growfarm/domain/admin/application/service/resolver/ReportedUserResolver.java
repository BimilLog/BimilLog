package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;

public interface ReportedUserResolver {
    User resolve(Long targetId);
    ReportType supports();
}
