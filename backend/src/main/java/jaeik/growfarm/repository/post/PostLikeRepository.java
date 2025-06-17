package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
 * 게시글 추천 Repository
 * 게시글 좋아요 관련 데이터베이스 작업을 수행하는 Repository
 * 수정일 : 2025-05-03
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    int countByPostId(Long postId);

    /**
     * <h3>글 추천 여부 조회</h3>
     * <p>특정 글에 대해 사용자가 추천했는지 여부를 조회한다.</p>
     *
     * @param postId 글 ID
     * @param userId 사용자 ID
     * @return 추천 정보가 존재하면 Optional에 포함된 PostLike 객체, 없으면 Optional.empty()
     * @author Jaeik
     * @since 1.0.0
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
}
