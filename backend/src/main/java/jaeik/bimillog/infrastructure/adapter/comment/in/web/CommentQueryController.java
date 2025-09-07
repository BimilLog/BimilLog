package jaeik.bimillog.infrastructure.adapter.comment.in.web;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.infrastructure.adapter.comment.dto.CommentDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        Page<CommentInfo> commentInfoPage = commentQueryUseCase.getCommentsOldestOrder(postId, pageable, userDetails);
        Page<CommentDTO> commentDtoPage = commentInfoPage.map(this::convertToCommentDTO);
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
    @GetMapping("/{postId}/popular")
    public ResponseEntity<List<CommentDTO>> getPopularComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {
        List<CommentInfo> commentInfoList = commentQueryUseCase.getPopularComments(postId, userDetails);
        List<CommentDTO> commentDtoList = commentInfoList.stream()
                .map(this::convertToCommentDTO)
                .toList();
        return ResponseEntity.ok(commentDtoList);
    }

    /**
     * <h3>CommentInfo를 CommentDTO로 변환</h3>
     *
     * @param commentInfo 변환할 도메인 객체
     * @return CommentDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public CommentDTO convertToCommentDTO(CommentInfo commentInfo) {
        CommentDTO commentDTO = new CommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getUserId(),
                commentInfo.getUserName(),
                commentInfo.getContent(),
                commentInfo.isDeleted(),
                commentInfo.getCreatedAt(),
                commentInfo.getParentId(),
                commentInfo.getLikeCount()
        );
        commentDTO.setPopular(commentInfo.isPopular());
        commentDTO.setUserLike(commentInfo.isUserLike());
        return commentDTO;
    }
}
