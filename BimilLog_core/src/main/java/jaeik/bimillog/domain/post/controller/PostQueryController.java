package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.service.PostQueryService;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.log.Log.LogLevel;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.dto.FullPostDTO;
import jaeik.bimillog.domain.post.dto.PostSearchDTO;
import jaeik.bimillog.domain.post.dto.SimplePostDTO;
import jaeik.bimillog.domain.post.controller.util.PostResponseMapper;
import jaeik.bimillog.domain.post.controller.util.PostViewCookieUtil;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 조회 컨트롤러</h2>
 * <p>Post 도메인의 조회(Query) 관련 REST API 엔드포인트를 제공하는 웹 어댑터입니다.</p>
 * <p>게시글 목록 조회, 상세 조회, 검색, 사용자 작성 게시글, 사용자 추천 게시글 API 제공</p>
 * <p>조회수 증가는 이벤트 기반으로 비동기 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostQueryController {

    private final PostQueryService postQueryService;
    private final PostResponseMapper postResponseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PostViewCookieUtil postViewCookieUtil;

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
    @Log(level = LogLevel.DEBUG,
         message = "게시판 목록 조회",
         logResult = false)
    public ResponseEntity<Page<SimplePostDTO>> getBoard(Pageable pageable) {
        Page<PostSimpleDetail> postList = postQueryService.getBoard(pageable);
        Page<SimplePostDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>게시글 상세 조회 API</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회합니다.</p>
     * <p>CQRS 패턴을 준수하여 순수한 조회 작업만 수행합니다.</p>
     * <p>조회 성공 시 PostViewedEvent를 발행하여 비동기로 조회수를 증가시킵니다.</p>
     *
     * @param postId      조회할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보 (Optional, 추천 여부 확인용)
     * @param request     HTTP 요청 (쿠키 확인용)
     * @param response    HTTP 응답 (쿠키 설정용)
     * @return 게시글 상세 정보 DTO (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{postId}")
    @Log(level = LogLevel.INFO,
         message = "게시글 상세 조회",
         logExecutionTime = true,
         excludeParams = {"request", "response", "userDetails"})
    public ResponseEntity<FullPostDTO> getPost(@PathVariable Long postId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        Long memberId = (userDetails != null) ? userDetails.getMemberId() : null;
        PostDetail postDetail = postQueryService.getPost(postId, memberId);
        FullPostDTO fullPostDTO = postResponseMapper.convertToFullPostResDTO(postDetail);
        
        // 중복 조회 검증 후 조회수 증가 이벤트 발행
         if (!postViewCookieUtil.hasViewed(request.getCookies(), postId)) {
             eventPublisher.publishEvent(new PostViewedEvent(postId));
             response.addCookie(postViewCookieUtil.createViewCookie(request.getCookies(), postId));
         } else {
             response.addCookie(postViewCookieUtil.createViewCookie(request.getCookies(), postId));
         }
        
        return ResponseEntity.ok(fullPostDTO);
    }

    /**
     * <h3>게시글 검색 API</h3>
     * <p>검증된 검색 조건으로 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param searchDTO 검색 조건 DTO (타입, 검색어 검증 포함)
     * @param pageable  페이지 정보
     * @return 검색된 게시글 목록 페이지 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/search")
    @Log(level = LogLevel.INFO,
         message = "게시글 검색",
         logExecutionTime = true,
         logResult = false)
    public ResponseEntity<Page<SimplePostDTO>> searchPost(@Valid @ModelAttribute PostSearchDTO searchDTO,
                                                          Pageable pageable) {
        Page<PostSimpleDetail> postList = postQueryService.searchPost(searchDTO.getType(), searchDTO.getTrimmedQuery(), pageable);
        Page<SimplePostDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>사용자가 작성한 게시글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 게시글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/me")
    public ResponseEntity<Page<SimplePostDTO>> getUserPosts(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSimpleDetail> postList = postQueryService.getMemberPosts(userDetails.getMemberId(), pageable);
        Page<SimplePostDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>사용자가 추천한 게시글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 게시글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/me/liked")
    public ResponseEntity<Page<SimplePostDTO>> getUserLikedPosts(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSimpleDetail> likedPosts = postQueryService.getMemberLikedPosts(userDetails.getMemberId(), pageable);
        Page<SimplePostDTO> dtoList = likedPosts.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }


}
