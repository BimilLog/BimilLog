package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.FeaturedPost;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * <h2>FeaturedPostRepository</h2>
 * <p>특집 게시글(주간 레전드, 레전드, 공지사항) JPA Repository 인터페이스입니다.</p>
 * <p>PostScheduledService에서 WEEKLY/LEGEND 갱신 시, PostAdminService에서 공지 토글 시 호출됩니다.</p>
 * <p>PostCacheFlag Enum을 DB 타입으로 재사용합니다 (REALTIME은 DB에 저장하지 않음).</p>
 *
 * @author Jaeik
 * @version 2.12.0
 */
public interface FeaturedPostRepository extends JpaRepository<FeaturedPost, Long> {

    /**
     * <h3>특집 게시글 존재 여부 확인</h3>
     * <p>특정 게시글이 특정 유형의 특집으로 이미 등록되어 있는지 확인합니다.</p>
     *
     * @param postId 게시글 ID
     * @param type   특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByPostIdAndType(Long postId, PostCacheFlag type);

    /**
     * <h3>특집 게시글 삭제</h3>
     * <p>특정 게시글의 특정 유형 특집을 삭제합니다.</p>
     * <p>공지 해제 시 NOTICE 유형 삭제에 사용됩니다.</p>
     *
     * @param postId 게시글 ID
     * @param type   특집 유형 (WEEKLY, LEGEND, NOTICE)
     */
    @Modifying
    @Query("DELETE FROM FeaturedPost fp WHERE fp.post.id = :postId AND fp.type = :type")
    void deleteByPostIdAndType(Long postId, PostCacheFlag type);

    /**
     * <h3>특정 유형의 모든 특집 게시글 삭제</h3>
     * <p>특정 유형의 모든 특집 게시글을 삭제합니다.</p>
     * <p>스케줄러에서 WEEKLY/LEGEND 전체 교체 시 사용됩니다.</p>
     *
     * @param type 특집 유형 (WEEKLY, LEGEND)
     */
    @Modifying
    @Query("DELETE FROM FeaturedPost fp WHERE fp.type = :type")
    void deleteAllByType(PostCacheFlag type);

    /**
     * <h3>특정 유형의 게시글 ID 목록 조회</h3>
     * <p>DB 폴백 시 featured_post 테이블에서 postId 목록을 조회합니다.</p>
     * <p>featuredAt 내림차순으로 정렬됩니다.</p>
     *
     * @param type 특집 유형 (WEEKLY, LEGEND, NOTICE)
     * @return 게시글 ID 목록
     */
    @Query("SELECT fp.post.id FROM FeaturedPost fp WHERE fp.type = :type ORDER BY fp.featuredAt DESC")
    List<Long> findPostIdsByType(PostCacheFlag type);
}
