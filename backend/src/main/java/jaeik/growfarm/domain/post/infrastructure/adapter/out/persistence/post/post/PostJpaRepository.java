package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.post;

import jaeik.growfarm.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h2>게시글 JPA Repository</h2>
 * <p>게시글 엔티티에 대한 데이터 접근을 처리하는 Spring Data JPA Repository 인터페이스입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostJpaRepository extends JpaRepository<Post, Long> {
}

