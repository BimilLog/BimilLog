package jaeik.growfarm.infrastructure.adapter.user.out.persistence.post;

import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.user.application.port.out.LoadPostPort;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 조회 어댑터</h2>
 * <p>User 도메인에서 Post 도메인의 In-Port를 통해 접근하는 어댑터</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component("userPostAdapter")
@RequiredArgsConstructor
public class PostAdapter implements LoadPostPort {

    private final PostQueryUseCase postQueryUseCase;

    @Override
    public Page<SimplePostResDTO> findPostsByUserId(Long userId, Pageable pageable) {
        return postQueryUseCase.getUserPosts(userId, pageable);
    }

    @Override
    public Page<SimplePostResDTO> findLikedPostsByUserId(Long userId, Pageable pageable) {
        return postQueryUseCase.getUserLikedPosts(userId, pageable);
    }
}