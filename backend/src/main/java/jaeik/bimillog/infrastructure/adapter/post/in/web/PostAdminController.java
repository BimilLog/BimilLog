package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 관리자 컨트롤러</h2>
 * <p>Post 도메인의 관리자 전용 REST API 엔드포인트를 제공하는 웹 어댑터입니다.</p>
 * <p>
 * 게시글 공지사항 등록/삭제
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
     * <h3>게시글 공지사항 등록/삭제 API (관리자용)</h3>
     * <p>게시글의 공지 설정을 토글합니다. 현재 공지이면 해제하고, 공지가 아니면 설정합니다.</p>
     * <p>관리자 권한이 필요합니다.</p>
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
