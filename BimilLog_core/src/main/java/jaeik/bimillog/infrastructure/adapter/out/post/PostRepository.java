package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * <h2>PostRepository</h2>
 * <p>Post 엔티티 JPA Repository 인터페이스입니다.</p>
 * <p>PostCommandAdapter에서 게시글 저장, 삭제, 조회수 증가 작업 시 호출됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * <h3>사용자 작성 게시글 ID 조회</h3>
     * <p>사용자가 작성한 모든 게시글의 ID 목록을 반환합니다.</p>
     * <p>캐시 무효화를 위해 사용됩니다 (Redis delete는 멱등성 보장)</p>
     *
     * @param memberId 게시글을 작성한 사용자 ID
     * @return List<Long> 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT p.id FROM Post p WHERE p.member.id = :memberId")
    List<Long> findIdsWithCacheFlagByMemberId(Long memberId);

    /**
     * <h3>회원 작성 게시글 일괄 삭제</h3>
     * <p>특정 사용자가 작성한 모든 게시글을 한 번에 삭제합니다.</p>
     *
     * @param memberId 게시글을 작성한 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("DELETE FROM Post p WHERE p.member.id = :memberId")
    void deleteAllByMemberId(Long memberId);

    /**
     * <h3>조회수 직접 증가 (최적화)</h3>
     * <p>게시글 ID를 통해 조회수를 1 증가시킵니다.</p>
     * <p>SELECT 없이 바로 UPDATE만 실행하여 성능을 최적화합니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Modifying
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :postId")
    void incrementViewsByPostId(Long postId);
}

