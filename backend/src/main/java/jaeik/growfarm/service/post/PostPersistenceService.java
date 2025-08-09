package jaeik.growfarm.service.post;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.repository.post.delete.PostDeleteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 영속성 서비스</h2>
 * <p>
 * 게시글의 생명주기 관리(수정, 삭제)를 담당하는 서비스
 * 트랜잭션 경계를 최소화하여 성능을 최적화한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
public class PostPersistenceService {
    
    private final PostDeleteRepository postDeleteRepository;

    /**
     * <h3>게시글 수정</h3>
     * <p>
     * 게시글 엔티티를 수정한다. JPA 더티 체킹을 이용한 업데이트.
     * </p>
     *
     * @param fullPostResDTO 수정할 게시글 정보
     * @param post    수정 대상 게시글 엔티티
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional
    public void updatePost(FullPostResDTO fullPostResDTO, Post post) {
        post.updatePost(fullPostResDTO);
    }

    /**
     * <h3>게시글 삭제</h3>
     * <p>
     * 게시글을 삭제한다. 삭제 대상이 인기글인 경우 Redis 캐시에서도 즉시 동기화 삭제한다.
     * 댓글은 CASCADE 설정으로 자동 삭제된다.
     * </p>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional
    public void deletePost(Long postId) {
        postDeleteRepository.deletePostWithCacheSync(postId);
    }
}