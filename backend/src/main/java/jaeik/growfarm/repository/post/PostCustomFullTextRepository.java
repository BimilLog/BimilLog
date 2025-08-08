package jaeik.growfarm.repository.post;

import jaeik.growfarm.entity.post.Post;
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
 * @version 1.1.0
 */
@Repository
public interface PostCustomFullTextRepository extends JpaRepository<Post, Long> {

    /**
     * <h3>공통 SELECT 절</h3>
     *
     * <p>
     * 게시글 조회 시 공통적으로 필요한 필드를 정의합니다.
     * </p>
     *
     * @since 1.1.0
     */
    String COMMON_SELECT_FIELDS = """
             p.post_id, p.title, p.views, p.is_notice, p.popular_flag, p.created_at, p.user_id, u.user_name
            """;

    /**
     * <h3>공통 FROM 절</h3>
     *
     * <p>
     * 게시글 조회 시 공통적으로 사용하는 JOIN 절입니다.
     * </p>
     *
     * @since 1.1.0
     */
    String COMMON_FROM_JOIN = """
            FROM post p
            LEFT JOIN users u ON p.user_id = u.user_id
            """;

    /**
     * <h3>공통 WHERE 조건 (공지글 제외)</h3>
     *
     * <p>
     * 공지글을 제외하는 공통 WHERE 조건입니다.
     * </p>
     *
     * @since 1.1.0
     */
    String COMMON_WHERE_CONDITION = """
            WHERE p.is_notice = false
            """;

    /**
     * <h3>COUNT 쿼리용 공통 FROM/WHERE</h3>
     *
     * <p>
     * COUNT 쿼리에서 사용하는 공통 FROM/WHERE 절입니다.
     * </p>
     *
     * @since 1.1.0
     */
    String COMMON_COUNT_FROM_WHERE = """
            FROM post p
            LEFT JOIN users u ON p.user_id = u.user_id
            WHERE p.is_notice = false
            """;

    /**
     * <h3>제목 전문검색</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND MATCH(p.title) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByTitleFullText(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>제목과 내용 전문검색</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByTitleContentFullText(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>작성자 검색 (4자 이상)</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND LOWER(u.user_name) LIKE LOWER(CONCAT(:keyword, '%'))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByAuthorStartsWith(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>작성자 검색 (4자 미만)</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND LOWER(u.user_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByAuthorContains(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>제목 LIKE 검색</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByTitleLike(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>제목+내용 LIKE 검색</h3>
     *
     * @param keyword 검색어
     * @param limit   페이지당 결과 개수
     * @param offset  페이지 시작 위치
     * @return 검색 결과
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT """ + COMMON_SELECT_FIELDS + """
            """ + COMMON_FROM_JOIN + """
            """ + COMMON_WHERE_CONDITION + """
            AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByTitleContentLike(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * <h3>제목 전문검색 개수 조회</h3>
     *
     * @param keyword 검색어
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 1.1.0
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
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM post p
            WHERE p.is_notice = false
            AND MATCH(p.title, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            """, nativeQuery = true)
    long countByTitleContentFullText(@Param("keyword") String keyword);

    /**
     * <h3>제목 LIKE 개수 조회</h3>
     *
     * @param keyword 검색어
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT COUNT(*)
            """ + COMMON_COUNT_FROM_WHERE + """
            AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """, nativeQuery = true)
    long countByTitleLike(@Param("keyword") String keyword);

    /**
     * <h3>제목+내용 LIKE 개수 조회</h3>
     *
     * @param keyword 검색어
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT COUNT(*)
            """ + COMMON_COUNT_FROM_WHERE + """
            AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """, nativeQuery = true)
    long countByTitleContentLike(@Param("keyword") String keyword);

    /**
     * <h3>작성자 검색 개수 조회 (4자 이상)</h3>
     *
     * @param keyword 검색어
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT COUNT(*)
            """ + COMMON_COUNT_FROM_WHERE + """
            AND LOWER(u.user_name) LIKE LOWER(CONCAT(:keyword, '%'))
            """, nativeQuery = true)
    long countByAuthorStartsWith(@Param("keyword") String keyword);

    /**
     * <h3>작성자 검색 개수 조회 (4자 미만)</h3>
     *
     * @param keyword 검색어
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 1.1.0
     */
    @Query(value = """
            SELECT COUNT(*)
            """ + COMMON_COUNT_FROM_WHERE + """
            AND LOWER(u.user_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """, nativeQuery = true)
    long countByAuthorContains(@Param("keyword") String keyword);
}