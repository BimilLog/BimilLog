package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostAdminController {

    private final PostAdminUseCase postAdminUseCase;

    /**
     * <h3>게시글 공지 설정 API (관리자용)</h3>
     * <p>특정 게시글을 공지로 설정합니다. 관리자 권한이 필요합니다.</p>
     *
     * @param postId 공지로 설정할 게시글 ID
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setPostAsNotice(@PathVariable Long postId) {
        postAdminUseCase.setPostAsNotice(postId);
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>게시글 공지 해제 API (관리자용)</h3>
     * <p>게시글의 공지 설정을 해제합니다. 관리자 권한이 필요합니다.</p>
     *
     * @param postId 공지 설정을 해제할 게시글 ID
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unsetPostAsNotice(@PathVariable Long postId) {
        postAdminUseCase.unsetPostAsNotice(postId);
        return ResponseEntity.ok().build();
    }
}
