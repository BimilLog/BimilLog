package jaeik.bimillog.domain.post.out;

import jaeik.bimillog.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>PostFulltextRepository</h2>
 * <p>MySQL FULLTEXT 인덱스를 활용한 전문 검색 쿼리를 제공하는 Repository 인터페이스입니다.</p>
 * <p>PostQueryAdapter에서 게시글 검색 시 한국어 ngram 파서를 통한 전문 검색 기능을 호출됩니다.</p>
 * <p>MySQL의 MATCH AGAINST 구문과 BOOLEAN MODE를 활용</p>
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
     * @return 검색 결과 (post_id, title, views, is_notice, created_at, user_id, user_name)
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.is_notice, p.created_at, p.member_id, m.member_name,
            (SELECT COUNT(pl.id) FROM post_like pl WHERE pl.post_id = p.post_id) AS like_count
            FROM post p
            LEFT JOIN member m ON p.member_id = m.member_id
            WHERE p.is_notice = false
            AND MATCH(p.title) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """, nativeQuery = true)
    List<Object[]> findByTitleFullText(@Param("keyword") String keyword, Pageable pageable, @Param("viewerId") Long viewerId);

    /**
     * <h3>제목과 내용 전문검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 제목과 내용에서 통합 검색합니다.</p>
     *
     * @param keyword  검색어 (BOOLEAN MODE용, 일반적으로 "검색어*" 형태)
     * @param pageable 페이지 정보
     * @return 검색 결과 (post_id, title, views, is_notice, created_at, user_id, user_name)
     * @author Jaeik
     * @since 2.0.0
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.is_notice, p.created_at, p.member_id, m.member_name,
            (SELECT COUNT(pl.id) FROM post_like pl WHERE pl.post_id = p.post_id) AS like_count
            FROM post p
            LEFT JOIN member m ON p.member_id = m.member_id
            WHERE p.is_notice = false
            AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """, nativeQuery = true)
    List<Object[]> findByTitleContentFullText(@Param("keyword") String keyword, Pageable pageable, @Param("viewerId") Long viewerId);

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
    long countByTitleFullText(@Param("keyword") String keyword, @Param("viewerId") Long viewerId);

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
    long countByTitleContentFullText(@Param("keyword") String keyword, @Param("viewerId") Long viewerId);

}
