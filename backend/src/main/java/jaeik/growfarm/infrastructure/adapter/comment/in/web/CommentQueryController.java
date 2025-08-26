package jaeik.growfarm.infrastructure.adapter.comment.in.web;

import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.entity.CommentInfo;
import jaeik.growfarm.domain.comment.entity.SimpleCommentInfo;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>댓글 Query 컨트롤러</h2>
 * <p>댓글을 조회하는 API를 담당합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentQueryController {

    private final CommentQueryUseCase commentQueryUseCase;
    private final CommentResponseMapper commentResponseMapper;

    /**
     * <h3>댓글 조회 API</h3>
     * <p>
     * 게시글에 달린 댓글을 과거순으로 페이징하여 반환한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param page   페이지 번호
     * @return 댓글 목록 페이지 (과거순)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{postId}")
    public ResponseEntity<Page<CommentDTO>> getComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page) {
        Page<CommentInfo> commentInfoPage = commentQueryUseCase.getCommentsOldestOrder(postId, page, userDetails);
        Page<CommentDTO> commentDtoPage = commentInfoPage.map(commentResponseMapper::convertToCommentDTO);
        return ResponseEntity.ok(commentDtoPage);
    }

    /**
     * <h3>인기댓글 조회 API</h3>
     * <p>
     * 추천수 3개 이상이며 글에서 추천수가 상위 3위이내인 댓글을 반환한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @return 인기댓글 리스트 (최대 3개)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{postId}/cache")
    public ResponseEntity<List<CommentDTO>> getPopularComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {
        List<CommentInfo> commentInfoList = commentQueryUseCase.getPopularComments(postId, userDetails);
        List<CommentDTO> commentDtoList = commentInfoList.stream()
                .map(commentResponseMapper::convertToCommentDTO)
                .toList();
        return ResponseEntity.ok(commentDtoList);
    }

    /**
     * <h3>도메인 객체를 DTO로 변환</h3>
     * <p>CommentInfo(도메인)를 CommentDTO로 변환합니다.</p>
     * <p>헥사고날 아키텍처에서 도메인 계층과 인프라스트럭처 계층을 분리하기 위한 변환 로직</p>
     *
     * @param commentInfo 도메인 댓글 정보
     * @return CommentDTO DTO 댓글 정보
     * @author Jaeik
     * @since 2.0.0
     */
}
