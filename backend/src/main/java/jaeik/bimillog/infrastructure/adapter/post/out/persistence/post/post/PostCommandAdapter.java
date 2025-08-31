package jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 커맨드 영속성 어댑터</h2>
 * <p>게시글 생성, 수정, 삭제와 관련된 데이터베이스 작업을 처리합니다.</p>
 * <p>PostCommandPort 인터페이스를 구현하여 게시글 명령 처리 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostCommandAdapter implements PostCommandPort {
    private final PostJpaRepository postJpaRepository;

    /**
     * <h3>게시글 저장</h3>
     * <p>새로운 게시글 엔티티를 데이터베이스에 저장합니다.</p>
     *
     * @param post 저장할 게시글 엔티티
     * @return 저장된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post save(Post post) {
        return postJpaRepository.save(post);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>주어진 게시글 엔티티를 데이터베이스에서 삭제합니다.</p>
     *
     * @param post 삭제할 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void delete(Post post) {
        postJpaRepository.delete(post);
    }

    /**
     * <h3>조회수 증가</h3>
     * <p>주어진 게시글의 조회수를 1 증가시키고 저장합니다.</p>
     *
     * @param post 조회수를 증가시킬 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementView(Post post) {
        post.incrementView();
        postJpaRepository.save(post);
    }
}
