package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 명령 어댑터</h2>
 * <p>게시글 명령 포트의 JPA 구현체입니다.</p>
 * <p>게시글 저장, 삭제</p>
 * <p>조회수 업데이트 쿼리 실행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostCommandAdapter implements PostCommandPort {
    private final PostRepository postRepository;

    /**
     * <h3>게시글 생성</h3>
     * <p>새로운 게시글 엔티티를 데이터베이스에 생성합니다.</p>
     * <p>{@link PostCommandService}에서 게시글 작성 시 호출됩니다.</p>
     *
     * @param post 생성할 게시글 엔티티
     * @return 생성된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post create(Post post) {
        return postRepository.save(post);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글 엔티티를 데이터베이스에서 삭제합니다.</p>
     * <p>{@link PostCommandService}에서 권한 검증 후 호출됩니다.</p>
     *
     * @param post 삭제할 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(Post post) {
        postRepository.delete(post);
    }

    /**
     * <h3>조회수 증가</h3>
     * <p>게시글 ID로 조회수를 1 증가시킵니다.</p>
     * <p>UPDATE 쿼리로 해당 게시글의 views 필드 증가</p>
     * <p>{@link PostCommandService}에서 게시글 조회 시 호출됩니다.</p>
     *
     * @param postId 조회수 증가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementViewByPostId(Long postId) {
        postRepository.incrementViewsByPostId(postId);
    }
}
