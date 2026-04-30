package jaeik.bimillog.domain.post.repository;

import jaeik.bimillog.domain.post.entity.jpa.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>PostFulltextRepository</h2>
 * <p>MySQL FULLTEXT 인덱스(ngram parser, token_size=2)를 활용한 전문 검색 쿼리를 제공합니다.</p>
 * <p>NATURAL LANGUAGE MODE 사용 — ngram 토큰을 그대로 매칭하여 한글 임의 위치 부분어 검색을 지원합니다.</p>
 * <p>BOOLEAN MODE prefix(`query+"*"`) 는 토큰 시퀀스 시작점만 매치하므로 한글 부분어 검색에 부적합합니다.</p>
 * <p>ORDER BY 는 동적으로 결정합니다 — Pageable.sort 가 비어있으면 created_at desc 기본,</p>
 * <p>지정되어 있으면 호출 측에서 ORDER BY 절을 조립해 별도 쿼리를 사용해야 합니다 (현 구현은 기본 정렬 단일).</p>
 * @author Jaeik
 * @version 2.8.0
 */
@Repository
public interface PostFulltextRepository extends JpaRepository<Post, Long> {

    /**
     * <h3>제목 전문검색 (NATURAL LANGUAGE MODE)</h3>
     * <p>MySQL FULLTEXT 인덱스(ngram parser)를 사용하여 제목에서 검색합니다.</p>
     *
     * @param keyword  검색어 (와일드카드 없이 raw query)
     * @param pageable 페이지 정보 (offset/limit 만 사용, sort 는 호출자가 별도 처리)
     * @return 검색 결과 (post_id, title, views, created_at, member_id, member_name, like_count, comment_count, is_weekly, is_legend, is_notice)
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.created_at, p.member_id, m.member_name,
            p.like_count, p.comment_count, p.is_weekly, p.is_legend, p.is_notice
            FROM post p
            LEFT JOIN member m ON p.member_id = m.member_id
            WHERE MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY p.created_at DESC
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """, nativeQuery = true)
    List<Object[]> findByTitleFullText(@Param("keyword") String keyword, Pageable pageable, @Param("viewerId") Long viewerId);

    /**
     * <h3>제목과 내용 전문검색 (NATURAL LANGUAGE MODE)</h3>
     * <p>MySQL FULLTEXT 인덱스(ngram parser)를 사용하여 제목과 내용에서 통합 검색합니다.</p>
     *
     * @param keyword  검색어 (와일드카드 없이 raw query)
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    @Query(value = """
            SELECT p.post_id, p.title, p.views, p.created_at, p.member_id, m.member_name,
            p.like_count, p.comment_count, p.is_weekly, p.is_legend, p.is_notice
            FROM post p
            LEFT JOIN member m ON p.member_id = m.member_id
            WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY p.created_at DESC
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """, nativeQuery = true)
    List<Object[]> findByTitleContentFullText(@Param("keyword") String keyword, Pageable pageable, @Param("viewerId") Long viewerId);

    /**
     * <h3>제목 전문검색 개수 조회</h3>
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM post p
            WHERE MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            """, nativeQuery = true)
    long countByTitleFullText(@Param("keyword") String keyword, @Param("viewerId") Long viewerId);

    /**
     * <h3>제목과 내용 전문검색 개수 조회</h3>
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM post p
            WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            """, nativeQuery = true)
    long countByTitleContentFullText(@Param("keyword") String keyword, @Param("viewerId") Long viewerId);

}
