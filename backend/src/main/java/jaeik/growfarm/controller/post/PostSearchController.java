package jaeik.growfarm.controller.post;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.service.post.search.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 검색 컨트롤러</h2>
 * <p>
 * 게시글 검색 기능을 담당
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/search")
public class PostSearchController {

    private final PostSearchService postSearchService;

    /**
     * <h3>게시글 검색 API</h3>
     *
     * <p>
     * 검색 유형과 검색어를 통해 게시글을 검색한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param type  검색 유형
     * @param query 검색어
     * @param page  페이지 번호
     * @param size  페이지 사이즈
     * @return 검색된 게시글 목록 페이지
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostResDTO>> searchPost(@RequestParam String type,
                                                             @RequestParam String query,
                                                             @RequestParam int page,
                                                             @RequestParam int size) {
        Page<SimplePostResDTO> searchList = postSearchService.searchPost(type, query, page, size);
        return ResponseEntity.ok(searchList);
    }
}