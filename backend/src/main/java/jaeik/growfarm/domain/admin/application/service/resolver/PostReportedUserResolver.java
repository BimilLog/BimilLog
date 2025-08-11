package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.post.application.port.out.LoadPostPort;
import jaeik.growfarm.domain.post.domain.Post;
import jaeik.growfarm.domain.admin.domain.ReportType;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReportedUserResolver implements ReportedUserResolver {

    private final LoadPostPort loadPostPort;

    @Override
    public User resolve(Long targetId) {
        Post post = loadPostPort.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return post.getUser();
    }

    @Override
    public ReportType supports() {
        return ReportType.POST;
    }
}
