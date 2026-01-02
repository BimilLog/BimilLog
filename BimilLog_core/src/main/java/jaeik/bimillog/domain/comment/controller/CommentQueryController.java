package jaeik.bimillog.domain.comment.controller;

import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>댓글 Query 컨트롤러</h2>
 * <p>댓글 도메인의 읽기 작업을 처리하는 REST API 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = Log.LogLevel.INFO, logExecutionTime = true, logParams = false, logResult = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentQueryController {
    private final CommentQueryService CommentQueryService;

    /**
     * <h3>댓글 조회 API</h3>
     * <p>지정된 게시글의 모든 댓글을 인기 댓글을 추출하고</p>
     * <p>일반댓글을 페이지별로 반환합니다.</p>
     *
     * @param postId 댓글을 조회할 게시글 ID
     * @return HTTP 응답 엔티티 (댓글 목록 페이지 데이터)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<Page<CommentInfo>> getComments(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @PathVariable Long postId, @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentInfo> commentInfoPage = CommentQueryService.findComments(postId, pageable, userDetails);
        return ResponseEntity.ok(commentInfoPage);
    }

    /**
     * <h3>인기댓글 조회 API</h3>
     * <p>지정된 게시글에서 추천수 3개 이상이며 상위 3위 이내의 댓글들을 인기댓글로 필터링하여 반환합니다.</p>
     * <p>추천수 기준 내림차순으로 정렬</p>
     *
     * @param postId 인기댓글을 조회할 게시글 ID
     * @return HTTP 응답 엔티티 (인기댓글 리스트, 최대 3개)
     */
    @GetMapping("/{postId}/popular")
    public ResponseEntity<List<CommentInfo>> getPopularComments(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                @PathVariable Long postId) {
        List<CommentInfo> commentInfoList = CommentQueryService.getPopularComments(postId, userDetails);
        return ResponseEntity.ok(commentInfoList);
    }
}
