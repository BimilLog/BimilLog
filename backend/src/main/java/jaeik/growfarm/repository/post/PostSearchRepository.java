package jaeik.growfarm.repository.post;

import jaeik.growfarm.dto.post.SimplePostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 검색 저장소</h2>
 * <p>
 * ISP(Interface Segregation Principle) 적용으로 분리된 인터페이스로, 게시글 검색 기능만 담당한다.
 * </p>
 *
 * @author jaeik
 * @version 1.1.0
 * @since 1.1.0
 */
@Repository
public interface PostSearchRepository {

    /**
     * <h3>게시글 목록 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색하며, 각 게시글의 총 댓글 수와 총 추천 수를 반환한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable);
}