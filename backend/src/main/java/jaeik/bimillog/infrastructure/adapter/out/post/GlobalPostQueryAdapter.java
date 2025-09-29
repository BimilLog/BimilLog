package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostQueryService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalPostQueryAdapter implements GlobalPostQueryPort {

    private final PostRepository postRepository;


    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     * <p>{@link PostQueryService}에서 게시글 존재성 검증 및 권한 확인 시 호출됩니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
    }
}
