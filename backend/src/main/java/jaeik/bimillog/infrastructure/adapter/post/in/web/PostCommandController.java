package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.entity.PostReqVO;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.Cookie;

/**
 * <h2>게시글 컨트롤러</h2>
 * <p>게시글 관련 API 요청을 처리하는 컨트롤러입니다.</p>
 * <p>게시글의 조회, 생성, 수정, 삭제 및 추천, 공지사항 설정/해제 기능을 제공합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostCommandController {

    private final PostCommandUseCase postCommandUseCase;
    private final PostInteractionUseCase postInteractionUseCase;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>게시글 작성 API</h3>
     * <p>새로운 게시글을 작성하고 저장합니다.</p>
     * <p>로그인 사용자는 자동으로 작성자 정보가 설정되고, 비로그인 사용자는 비밀번호를 통해 익명으로 작성할 수 있습니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (Optional - 비로그인 사용자 허용)
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 생성된 게시글의 URI를 포함한 응답 (201 Created)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping
    public ResponseEntity<Void> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody @Valid PostReqDTO postReqDTO) {
        PostReqVO postReqVO = convertToPostReqVO(postReqDTO);
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        Long postId = postCommandUseCase.writePost(userId, postReqVO);
        return ResponseEntity.created(URI.create("/api/posts/" + postId)).build();
    }

    /**
     * <h3>게시글 수정 API</h3>
     * <p>게시글 작성자만 게시글을 수정할 수 있습니다.</p>
     *
     * @param postId      수정할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  수정할 게시글 정보 DTO
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody @Valid PostReqDTO postReqDTO) {
        PostReqVO postReqVO = convertToPostReqVO(postReqDTO);
        postCommandUseCase.updatePost(userDetails.getUserId(), postId, postReqVO);
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>게시글 삭제 API</h3>
     * <p>게시글 작성자만 게시글을 삭제할 수 있습니다.</p>
     *
     * @param postId      삭제할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 성공 응답 (204 No Content)
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        postCommandUseCase.deletePost(userDetails.getUserId(), postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * <h3>게시글 추천/추천 취소 API</h3>
     * <p>게시글에 추천를 누르거나 추천를 취소합니다.</p>
     *
     * @param postId      추천/추천 취소할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        postInteractionUseCase.likePost(userDetails.getUserId(), postId);
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>게시글 조회수 증가 API</h3>
     * <p>게시글 조회 시 조회수를 증가시킵니다.</p>
     * <p>쿠키 기반으로 중복 조회를 방지합니다.</p>
     * <p>헥사고날 아키텍처를 준수하여 HTTP 데이터를 도메인 데이터로 변환합니다.</p>
     *
     * @param postId   조회수를 증가시킬 게시글 ID
     * @param request  HTTP 요청 (쿠키 확인용)
     * @param response HTTP 응답 (쿠키 설정용)
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/{postId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long postId,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        // HTTP 데이터를 도메인 데이터로 변환
        String userIdentifier = extractUserIdentifier(request);
        Map<String, String> viewHistory = extractViewHistory(request);
        
        // 이벤트 발행 - 헥사고날 아키텍처 준수
        eventPublisher.publishEvent(new PostViewedEvent(this, postId, userIdentifier, viewHistory));
        
        // 업데이트된 조회 이력을 쿠키에 설정 (비동기 처리 완료 후)
        // Note: 실제로는 이벤트 처리 결과를 받아서 쿠키를 설정해야 하지만,
        // 현재는 비동기 처리이므로 기존 방식을 유지
        postInteractionUseCase.incrementViewCountWithCookie(postId, request, response);
        
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>PostReqDTO를 PostReqVO로 변환</h3>
     *
     * @param dto 변환할 DTO 객체
     * @return PostReqVO 도메인 value object
     * @author jaeik
     * @since 2.0.0
     */
    private PostReqVO convertToPostReqVO(PostReqDTO dto) {
        Integer passwordInt = null;
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            passwordInt = Integer.parseInt(dto.getPassword());
        }
        
        return PostReqVO.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .password(passwordInt)
                .build();
    }

    /**
     * <h3>사용자 식별자 추출</h3>
     * <p>HTTP 요청에서 사용자를 식별할 수 있는 값을 추출합니다.</p>
     * <p>IP 주소를 기본으로 사용하며, 필요시 다른 식별자를 추가할 수 있습니다.</p>
     *
     * @param request HTTP 요청
     * @return 사용자 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    private String extractUserIdentifier(HttpServletRequest request) {
        // X-Forwarded-For 헤더가 있으면 사용 (프록시 환경 고려)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP.trim();
        }
        
        // 기본적으로 RemoteAddr 사용
        return request.getRemoteAddr();
    }

    /**
     * <h3>조회 이력 추출</h3>
     * <p>HTTP 쿠키에서 사용자의 게시글 조회 이력을 추출합니다.</p>
     * <p>쿠키가 없는 경우 빈 맵을 반환합니다.</p>
     *
     * @param request HTTP 요청
     * @return 조회 이력 맵
     * @author Jaeik
     * @since 2.0.0
     */
    private Map<String, String> extractViewHistory(HttpServletRequest request) {
        Map<String, String> viewHistory = new HashMap<>();
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(cookie -> "post_views".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> {
                        try {
                            String cookieValue = cookie.getValue();
                            if (cookieValue != null && !cookieValue.trim().isEmpty()) {
                                viewHistory.put("viewed_posts", cookieValue);
                            }
                        } catch (Exception e) {
                            // 쿠키 파싱 오류 시 빈 이력으로 처리
                            viewHistory.put("viewed_posts", "");
                        }
                    });
        }
        
        // 기본값 설정
        viewHistory.putIfAbsent("viewed_posts", "");
        
        return viewHistory;
    }
}

