package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;

public interface ReportedUserResolver {
    User resolve(Long targetId);
    ReportType supports();
}
