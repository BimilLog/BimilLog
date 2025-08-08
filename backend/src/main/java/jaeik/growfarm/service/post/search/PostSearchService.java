package jaeik.growfarm.service.post.search;

import jaeik.growfarm.dto.post.SimplePostDTO;
import org.springframework.data.domain.Page;

/**
 * <h2>게시글 검색 서비스 인터페이스</h2>
 * <p>
 * 게시글 검색 기능을 담당하는 서비스 인터페이스
 * </p>
 * 
 * @author Jaeik
 * @version 1.1.0
 * @since 1.1.0
 */
public interface PostSearchService {

    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색 유형과 검색어를 통해 게시글을 검색하고 최신순으로 페이지네이션한다.
     * </p>
     *
     * @param type  검색 유형
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 사이즈
     * @return 검색된 게시글 목록 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    Page<SimplePostDTO> searchPost(String type, String query, int page, int size);
}