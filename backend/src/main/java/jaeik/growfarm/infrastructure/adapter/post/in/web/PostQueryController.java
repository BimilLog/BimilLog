package jaeik.growfarm.infrastructure.adapter.post.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.PostDetail;
import jaeik.growfarm.domain.post.entity.PostSearchResult;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostQueryController {

    private final PostQueryUseCase postQueryUseCase;
    private final PostInteractionUseCase postInteractionUseCase;
    private final PostResponseMapper postResponseMapper;

    /**
     * <h3>게시판 목록 조회 API</h3>
     * <p>최신순으로 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostResDTO>> getBoard(Pageable pageable) {
        Page<PostSearchResult> postList = postQueryUseCase.getBoard(pageable);
        Page<SimplePostResDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>게시글 상세 조회 API</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회하고 조회수를 증가시킵니다.</p>
     * <p>쿠키 기반으로 중복 조회를 방지하여 조회수를 증가시킵니다.</p>
     * <p>count 파라미터를 통해 조회수 증가 여부를 제어할 수 있습니다.</p>
     *
     * @param postId      조회할 게시글 ID
     * @param count       조회수 증가 여부 (기본값: true)
     * @param userDetails 현재 로그인한 사용자 정보 (Optional, 추천 여부 확인용)
     * @param request     HTTP 요청 (쿠키 확인용)
     * @param response    HTTP 응답 (쿠키 설정용)
     * @return 게시글 상세 정보 DTO (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{postId}")
    public ResponseEntity<FullPostResDTO> getPost(@PathVariable Long postId,
                                                  @RequestParam(name = "count", defaultValue = "true") boolean count,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        if (count) {
            postInteractionUseCase.incrementViewCountWithCookie(postId, request, response);
        }

        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        PostDetail postDetail = postQueryUseCase.getPost(postId, userId);
        FullPostResDTO fullPostResDTO = postResponseMapper.convertToFullPostResDTO(postDetail);
        return ResponseEntity.ok(fullPostResDTO);
    }

    /**
     * <h3>게시글 검색 API</h3>
     * <p>검색 유형(type)과 검색어(query)를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param type     검색 유형 (예: title, content, writer)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostResDTO>> searchPost(@RequestParam String type,
                                                             @RequestParam String query,
                                                             Pageable pageable) {
        Page<PostSearchResult> postList = postQueryUseCase.searchPost(type, query, pageable);
        Page<SimplePostResDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }



}
