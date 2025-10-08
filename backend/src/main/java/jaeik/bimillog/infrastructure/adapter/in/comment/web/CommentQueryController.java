package jaeik.bimillog.infrastructure.adapter.in.comment.web;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentDTO;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.SimpleCommentDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>댓글 Query 컨트롤러</h2>
 * <p>
 * 댓글 도메인의 읽기 작업을 처리하는 REST API 어댑터입니다.
 * </p>
 * <p>
 * 클라이언트로부터 댓글 조회 요청을 받아 도메인 계층의 CommentQueryUseCase에서 데이터를 가져오고,
 * CQRS 패턴에서 Query 책임을 담당하며 읽기 전용 최적화된 응답을 제공합니다.
 * </p>
 * <p>
 * 처리하는 HTTP 요청: GET /api/comment/{postId}, /api/comment/{postId}/popular, /api/comment/me, /api/comment/me/liked
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentQueryController {

    private final CommentQueryUseCase commentQueryUseCase;

    /**
     * <h3>댓글 조회 API</h3>
     * <p>지정된 게시글의 모든 댓글을 오래된 순서로 정렬하여 페이지별로 반환합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (좋아요 상태 확인용, 선택사항)
     * @param postId 댓글을 조회할 게시글 ID
     * @param page 페이지 번호 (기본값 0, 20개씩 페이징)
     * @return HTTP 응답 엔티티 (댓글 목록 페이지 데이터)
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
     * <p>지정된 게시글에서 추천수 3개 이상이며 상위 5위 이내의 댓글들을 인기댓글로 필터링하여 반환합니다.</p>
     * <p>추천수 기준 내림차순으로 정렬</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (좋아요 상태 확인용, 선택사항)
     * @param postId 인기댓글을 조회할 게시글 ID
     * @return HTTP 응답 엔티티 (인기댓글 리스트, 최대 5개)
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
     * <h3>사용자가 작성한 댓글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/me")
    public ResponseEntity<Page<SimpleCommentDTO>> getUserComments(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimpleCommentInfo> commentInfoList = commentQueryUseCase.getMemberComments(userDetails.getMemberId(), pageable);
        Page<SimpleCommentDTO> commentList = commentInfoList.map(this::convertToSimpleCommentDTO);
        return ResponseEntity.ok(commentList);
    }

    /**
     * <h3>사용자가 추천한 댓글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 댓글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/me/liked")
    public ResponseEntity<Page<SimpleCommentDTO>> getUserLikedComments(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimpleCommentInfo> likedCommentsInfo = commentQueryUseCase.getMemberLikedComments(userDetails.getMemberId(), pageable);
        Page<SimpleCommentDTO> likedComments = likedCommentsInfo.map(this::convertToSimpleCommentDTO);
        return ResponseEntity.ok(likedComments);
    }

    /**
     * <h3>도메인 객체를 DTO로 변환</h3>
     *
     * @param commentInfo 도메인 계층 댓글 정보 객체
     * @return CommentDTO 웹 계층 댓글 응답 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentDTO convertToCommentDTO(CommentInfo commentInfo) {
        CommentDTO commentDTO = new CommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getMemberId(),
                commentInfo.getMemberName(),
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

    /**
     * <h3>SimpleCommentInfo를 SimpleCommentDTO로 변환</h3>
     *
     * @param commentInfo 변환할 도메인 객체
     * @return SimpleCommentDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public SimpleCommentDTO convertToSimpleCommentDTO(SimpleCommentInfo commentInfo) {
        return new SimpleCommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getMemberName(),
                commentInfo.getContent(),
                commentInfo.getCreatedAt(),
                commentInfo.getLikeCount(),
                commentInfo.isUserLike()
        );
    }
}
