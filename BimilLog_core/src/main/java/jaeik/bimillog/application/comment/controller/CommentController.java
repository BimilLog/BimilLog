package jaeik.bimillog.application.comment.controller;

import jaeik.bimillog.application.comment.dto.CommentDTO;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>댓글 컨트롤러</h2>
 * <p>BFF방식의 컨트롤러</p>
 * <p>댓글 조회 + 인기 댓글 조회</p>
 * @author Jaeik
 * @version 2.7.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentQueryService CommentQueryService;

    /**
     * <h3>댓글 조회 API</h3>
     * <p>일반댓글 페이징 + 인기 댓글 리스트</p>
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return 댓글 DTO
     */
    @GetMapping("/{postId}")
    public ResponseEntity<CommentDTO> getComments(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        CommentDTO commentDTO = CommentQueryService.findComments(postId, pageable, userDetails);
        return ResponseEntity.ok(commentDTO);
    }
}
