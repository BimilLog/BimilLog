package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>PostAdminController</h2>
 * <p>
 * Post 도메인의 관리자 전용 REST API 엔드포인트를 제공하는 웹 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 관리자 웹 요청을 도메인 비즈니스 로직으로 연결하며,
 * Spring Security의 PreAuthorize를 통해 ADMIN 권한을 엄격히 검증합니다.
 * </p>
 * <p>
 * 관리자 페이지에서 게시글 공지사항 설정/해제 요청 시 호출되어 공지 상태를 변경하고,
 * 캐시 동기화 실패를 격리하여 핵심 비즈니스 로직에 영향을 주지 않도록 처리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Slf4j
public class PostAdminController {

    private final PostAdminUseCase postAdminUseCase;
    private final PostCacheUseCase postCacheUseCase;

    /**
     * <h3>게시글 공지 토글 API (관리자용)</h3>
     * <p>게시글의 공지 설정을 토글합니다. 현재 공지이면 해제하고, 공지가 아니면 설정합니다.</p>
     * <p>관리자 권한이 필요합니다.</p>
     * <p>DB 업데이트와 캐시 동기화를 분리하여 캐시 실패가 핵심 로직에 영향을 주지 않도록 합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> togglePostNotice(@PathVariable Long postId) {
        // 1. 핵심 비즈니스 로직 실행 (DB 업데이트)
        postAdminUseCase.togglePostNotice(postId);
        
        // 2. 캐시 동기화 (실패 격리)
        try {
            // 현재 공지 상태를 다시 조회하여 캐시 동기화
            boolean isCurrentlyNotice = postAdminUseCase.isPostNotice(postId);
            postCacheUseCase.syncNoticeCache(postId, isCurrentlyNotice);
        } catch (Exception e) {
            // 캐시 동기화 실패는 로그만 남기고 API 응답에는 영향 없음
            log.warn("공지사항 캐시 동기화 실패: postId={}, error={}", postId, e.getMessage());
        }
        
        return ResponseEntity.ok().build();
    }
}
