package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>게시글 전문검색 쿼리 Repository</h2>
 * <p>
 * MySQL MATCH AGAINST를 사용한 전문검색 쿼리들
 * </p>
 *
 * @author Jaeik
 * @version 1.0
 */
@Repository
public interface PostCustomFullTextRepository extends JpaRepository<Post, Long> {

        /**
         * <h3>제목 전문검색</h3>
         *
         * @param keyword  검색어
         * @param pageable 페이지 정보
         * @return 검색 결과
         */
        @Query(value = """
                        SELECT p.post_id, p.title, p.views, p.is_notice, p.popular_flag, p.created_at, p.user_id, u.user_name
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND MATCH(p.title) AGAINST(:keyword IN BOOLEAN MODE)
                        """, nativeQuery = true)
        List<Object[]> findByTitleFullText(@Param("keyword") String keyword, Pageable pageable);

        /**
         * <h3>제목과 내용 전문검색</h3>
         *
         * @param keyword  검색어
         * @param pageable 페이지 정보
         * @return 검색 결과
         */
        @Query(value = """
                        SELECT p.post_id, p.title, p.views, p.is_notice, p.popular_flag, p.created_at, p.user_id, u.user_name
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
                        """, nativeQuery = true)
        List<Object[]> findByTitleContentFullText(@Param("keyword") String keyword, Pageable pageable);

        /**
         * <h3>작성자 검색 (4자 이상)</h3>
         *
         * @param keyword  검색어
         * @param pageable 페이지 정보
         * @return 검색 결과
         */
        @Query(value = """
                        SELECT p.post_id, p.title, p.views, p.is_notice, p.popular_flag, p.created_at, p.user_id, u.user_name
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND LOWER(u.user_name) LIKE LOWER(CONCAT(:keyword, '%'))
                        """, nativeQuery = true)
        List<Object[]> findByAuthorStartsWith(@Param("keyword") String keyword, Pageable pageable);

        /**
         * <h3>작성자 검색 (4자 미만)</h3>
         *
         * @param keyword  검색어
         * @param pageable 페이지 정보
         * @return 검색 결과
         */
        @Query(value = """
                        SELECT p.post_id, p.title, p.views, p.is_notice, p.popular_flag, p.created_at, p.user_id, u.user_name
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND LOWER(u.user_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        """, nativeQuery = true)
        List<Object[]> findByAuthorContains(@Param("keyword") String keyword, Pageable pageable);

        /**
         * <h3>제목 전문검색 개수 조회</h3>
         *
         * @param keyword 검색어
         * @return 검색 결과 개수
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
         *
         * @param keyword 검색어
         * @return 검색 결과 개수
         */
        @Query(value = """
                        SELECT COUNT(*)
                        FROM post p
                        WHERE p.is_notice = false
                        AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
                        """, nativeQuery = true)
        long countByTitleContentFullText(@Param("keyword") String keyword);

        /**
         * <h3>작성자 검색 개수 조회 (4자 이상)</h3>
         *
         * @param keyword 검색어
         * @return 검색 결과 개수
         */
        @Query(value = """
                        SELECT COUNT(*)
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND LOWER(u.user_name) LIKE LOWER(CONCAT(:keyword, '%'))
                        """, nativeQuery = true)
        long countByAuthorStartsWith(@Param("keyword") String keyword);

        /**
         * <h3>작성자 검색 개수 조회 (4자 미만)</h3>
         *
         * @param keyword 검색어
         * @return 검색 결과 개수
         */
        @Query(value = """
                        SELECT COUNT(*)
                        FROM post p
                        LEFT JOIN users u ON p.user_id = u.user_id
                        WHERE p.is_notice = false
                        AND LOWER(u.user_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        """, nativeQuery = true)
        long countByAuthorContains(@Param("keyword") String keyword);
}