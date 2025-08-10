package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportedUserResolverService {

    private final Map<ReportType, ReportedUserResolver> resolvers = new EnumMap<>(ReportType.class);

    public ReportedUserResolverService(List<ReportedUserResolver> resolverList) {
        for (ReportedUserResolver resolver : resolverList) {
            resolvers.put(resolver.supports(), resolver);
        }
    }

    public Users resolveUser(ReportType reportType, Long targetId) {
        ReportedUserResolver resolver = resolvers.get(reportType);
        if (resolver == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }
        return resolver.resolve(targetId);
    }
}
