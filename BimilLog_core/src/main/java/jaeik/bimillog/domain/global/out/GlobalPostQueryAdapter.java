package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.post.out.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>전역 게시글 조회 어댑터</h2>
 * <p>도메인 경계를 넘어 게시글 엔티티를 조회하는 전역 포트 구현체입니다.</p>
 * <p>여러 도메인에서 공통으로 필요한 게시글 조회 기능을 제공합니다.</p>
 * <p>핵사고날 아키텍처에서 도메인 간 직접 참조를 방지하면서 필요한 데이터 접근을 가능하게 합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class GlobalPostQueryAdapter {

    private final PostRepository postRepository;


    /**
     * <h3>ID로 게시글 조회</h3>
     * <p>주어진 ID를 사용하여 게시글을 조회합니다.</p>
     * <p>PostCommandService, PostAdminService, PostInteractionService, PostQueryService,
     * CommentCommandService, AdminCommandService 등
     * 여러 도메인 서비스에서 게시글 엔티티 조회 시 호출됩니다.</p>
     *
     * @param id 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
    }
}
