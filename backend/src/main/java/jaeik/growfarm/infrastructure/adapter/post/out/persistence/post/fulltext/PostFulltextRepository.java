package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.fulltext;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jaeik.growfarm.domain.post.entity.Post;
import java.util.List;

/**
 * <h2>게시글 전문검색 쿼리 Repository</h2>
 * <p>
 * MySQL MATCH AGAINST를 사용한 전문검색 쿼리들
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface PostFulltextRepository extends JpaRepository<Post, Long> {

    /**
     * <h3>제목 전문검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 제목에서 검색합니다.</p>
     *
     * @param keyword  검색어 (BOOLEAN MODE용, 일반적으로 "검색어*" 형태)
     * @param pageable 페이지 정보
     * @return 검색 결과 (post_id, title, views, is_notice, post_cache_flag, created_at, user_id, user_name)
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.is_notice, p.post_cache_flag, p.created_at, p.user_id, u.user_name
            FROM post p
            LEFT JOIN users u ON p.user_id = u.user_id
            WHERE p.is_notice = false
            AND MATCH(p.title) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<Object[]> findByTitleFullText(@Param("keyword") String keyword, Pageable pageable);

    /**
     * <h3>제목과 내용 전문검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 제목과 내용에서 통합 검색합니다.</p>
     *
     * @param keyword  검색어 (BOOLEAN MODE용, 일반적으로 "검색어*" 형태)
     * @param pageable 페이지 정보
     * @return 검색 결과 (post_id, title, views, is_notice, post_cache_flag, created_at, user_id, user_name)
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.is_notice, p.post_cache_flag, p.created_at, p.user_id, u.user_name
            FROM post p
            LEFT JOIN users u ON p.user_id = u.user_id
            WHERE p.is_notice = false
            AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<Object[]> findByTitleContentFullText(@Param("keyword") String keyword, Pageable pageable);

    /**
     * <h3>제목 전문검색 개수 조회</h3>
     * <p>페이지네이션을 위한 총 검색 결과 개수를 조회합니다.</p>
     *
     * @param keyword 검색어 (BOOLEAN MODE용)
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM post p
            WHERE p.is_notice = false
            AND MATCH(p.title) AGAINST(:keyword IN BOOLEAN MODE)
            """, nativeQuery = true)
    long countByTitleFullText(@Param("keyword") String keyword);

    /**
     * <h3>제목과 내용 전문검색 개수 조회</h3>
     * <p>페이지네이션을 위한 총 검색 결과 개수를 조회합니다.</p>
     *
     * @param keyword 검색어 (BOOLEAN MODE용)
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM post p
            WHERE p.is_notice = false
            AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            """, nativeQuery = true)
    long countByTitleContentFullText(@Param("keyword") String keyword);

}