package jaeik.growfarm.service.post.search;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.repository.post.search.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * <h2>게시글 검색 서비스 구현체</h2>
 * <p>
 * 게시글 검색 기능을 구현하는 서비스 클래스
 * </p>
 * 
 * @author Jaeik
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private final PostSearchRepository postSearchRepository;

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
    @Override
    public Page<SimplePostResDTO> searchPost(String type, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postSearchRepository.searchPosts(query, type, pageable);
    }
}