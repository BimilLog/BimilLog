package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.post.dto.CursorPageResponse;
import jaeik.bimillog.domain.post.dto.PostSearchDTO;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.service.PostQueryService;
import jaeik.bimillog.domain.post.service.PostSearchService;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.log.Log.LogLevel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 조회 컨트롤러</h2>
 * <p>Post 도메인의 조회(Query) 관련 REST API 엔드포인트를 제공하는 웹 어댑터입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색, 사용자 작성 게시글, 사용자 추천 게시글 API 제공</p>
 * <p>조회수 중복 방지는 Redis SET 기반, 조회수 버퍼링은 Redis Hash 기반으로 처리</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Slf4j
public class PostQueryController {
    private final PostQueryService postQueryService;
    private final PostSearchService postSearchService;

    /**
     * <h3>게시판 목록 조회 API</h3>
     * <p>게시글 목록을 최신순 조회합니다.</p>
     * <p>회원은 블랙리스트 필터링이 적용되고, 비회원은 전체 조회됩니다.</p>
     *
     * @param cursor 마지막으로 조회한 게시글 ID (null이면 처음부터)
     * @param size   조회할 개수 (기본값: 20)
     * @return CursorPageResponse 커서 기반 페이지 응답 (200 OK)
     */
    @GetMapping
    @Log(level = LogLevel.DEBUG,
         message = "게시판 목록 조회",
         logResult = false)
    public ResponseEntity<CursorPageResponse<PostSimpleDetail>> getBoard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                         @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "20") int size) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        CursorPageResponse<PostSimpleDetail> postList = postQueryService.getBoardByCursor(cursor, size, memberId);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>게시글 상세 조회 API</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회합니다.</p>
     * <p>Redis SET으로 24시간 중복 조회를 방지하고, Redis Hash에 조회수를 버퍼링합니다.</p>
     *
     * @param postId      조회할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보 (Optional, 추천 여부 확인용)
     * @param request     HTTP 요청 (IP 추출용)
     * @return 게시글 상세 정보 DTO (200 OK)
     */
    @GetMapping("/{postId}")
    @Log(level = LogLevel.INFO,
         message = "게시글 상세 조회",
         logExecutionTime = true,
         excludeParams = {"request", "userDetails"})
    public ResponseEntity<PostDetail> getPost(@PathVariable Long postId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails,
                                               HttpServletRequest request) {
        Long memberId = (userDetails != null) ? userDetails.getMemberId() : null;
        String viewerKey = buildViewerKey(memberId, request);
        PostDetail postDetail = postQueryService.getPost(postId, memberId, viewerKey);

        return ResponseEntity.ok(postDetail);
    }

    /**
     * <h3>게시글 검색 API</h3>
     * <p>검증된 검색 조건으로 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param searchDTO 검색 조건 DTO (타입, 검색어 검증 포함)
     * @param pageable  페이지 정보
     * @return 검색된 게시글 목록 페이지 (200 OK)
     */
    @GetMapping("/search")
    @Log(level = LogLevel.INFO,
         message = "게시글 검색",
         logExecutionTime = true,
         logResult = false)
    public ResponseEntity<Page<PostSimpleDetail>> searchPost(@Valid @ModelAttribute PostSearchDTO searchDTO,
                                                          Pageable pageable,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        Page<PostSimpleDetail> postList = postSearchService.searchPost(searchDTO.getType(), searchDTO.getTrimmedQuery(), pageable, memberId);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>조회자 키 생성</h3>
     * <p>로그인 사용자는 m:{memberId}, 비로그인 사용자는 ip:{clientIp} 형태로 생성합니다.</p>
     */
    private String buildViewerKey(Long memberId, HttpServletRequest request) {
        if (memberId != null) {
            return "m:" + memberId;
        }
        return "ip:" + extractClientIp(request);
    }

    /**
     * <h3>클라이언트 IP 추출</h3>
     * <p>X-Forwarded-For 헤더 우선, 없으면 request.getRemoteAddr() 사용</p>
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
