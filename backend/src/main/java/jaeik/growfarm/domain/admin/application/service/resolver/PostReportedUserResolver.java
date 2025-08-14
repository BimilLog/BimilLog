package jaeik.growfarm.domain.admin.application.service.resolver;

import jaeik.growfarm.domain.post.application.port.out.PostQueryPort;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.admin.entity.ReportType;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 신고 사용자 해결사</h2>
 * <p>게시글 신고 유형에 대해 신고 대상 사용자 정보를 해결하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostReportedUserResolver implements ReportedUserResolver {

    private final PostQueryPort postQueryPort;

    /**
     * <h3>게시글 ID로 신고 대상 사용자 해결</h3>
     * <p>주어진 게시글 ID에 해당하는 게시글의 작성자(사용자)를 조회하여 반환합니다.</p>
     *
     * @param targetId 게시글 ID
     * @return User 게시글 작성 사용자 엔티티
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public User resolve(Long targetId) {
        Post post = postQueryPort.findById(targetId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return post.getUser();
    }

    /**
     * <h3>지원하는 신고 유형 반환</h3>
     * <p>이 해결사가 지원하는 신고 유형(POST)을 반환합니다.</p>
     *
     * @return ReportType.POST
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ReportType supports() {
        return ReportType.POST;
    }
}
