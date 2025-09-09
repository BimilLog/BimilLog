package jaeik.bimillog.infrastructure.adapter.post.out.post;

import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>PostCommandAdapter</h2>
 * <p>
 * Post 도메인의 명령 포트를 JPA 기술로 구현하는 아웃바운드 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 도메인과 영속성 기술을 분리하여 도메인의 순수성을 보장하고,
 * PostCommandPort 인터페이스를 통해 비즈니스 로직에서 필요한 게시글 저장 및 삭제 기능을 제공합니다.
 * </p>
 * <p>
 * PostCommandService에서 게시글 생성, 수정, 삭제 처리 시 호출되어 데이터베이스 영속성을 담당하고,
 * 조회수 증가 시에는 최적화된 UPDATE 쿼리를 직접 실행하여 성능을 개선합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PostCommandAdapter implements PostCommandPort {
    private final PostRepository postRepository;

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
        return postRepository.save(post);
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
        postRepository.delete(post);
    }

    /**
     * <h3>조회수 직접 증가 (최적화)</h3>
     * <p>게시글 ID를 통해 직접 조회수를 1 증가시킵니다.</p>
     * <p>불필요한 SELECT 쿼리 없이 바로 UPDATE 쿼리만 실행하여 성능을 최적화합니다.</p>
     * <p>Controller에서 이미 게시글 존재 여부를 검증했으므로 추가 존재 검사는 하지 않습니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void incrementViewByPostId(Long postId) {
        postRepository.incrementViewsByPostId(postId);
    }
}
