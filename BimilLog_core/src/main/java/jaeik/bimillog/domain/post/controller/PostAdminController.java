package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.post.dto.PostNoticeUpdateDTO;
import jaeik.bimillog.domain.post.service.PostAdminService;
import jaeik.bimillog.infrastructure.log.Log;
import jakarta.validation.Valid;
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
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Slf4j
public class PostAdminController {
    private final PostAdminService postAdminService;

    /**
     * <h3>게시글 공지사항 설정/해제 API (관리자용)</h3>
     * <p>게시글의 공지 설정을 토글합니다. 현재 공지이면 해제하고, 공지가 아니면 설정합니다.</p>
     * <p>관리자 권한이 필요합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> togglePostNotice(@Valid @RequestBody PostNoticeUpdateDTO postNoticeUpdateDTO) {
        postAdminService.togglePostNotice(postNoticeUpdateDTO.getPostId(), postNoticeUpdateDTO.isNotice());
        return ResponseEntity.ok().build();
    }
}
