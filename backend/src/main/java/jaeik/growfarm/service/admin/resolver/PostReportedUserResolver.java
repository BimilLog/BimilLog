package jaeik.growfarm.service.admin.resolver;

import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.report.ReportType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReportedUserResolver implements ReportedUserResolver {

    private final PostRepository postRepository;

    @Override
    public Users resolve(Long targetId) {
        Post post = postRepository.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return post.getUser();
    }

    @Override
    public ReportType supports() {
        return ReportType.POST;
    }
}
