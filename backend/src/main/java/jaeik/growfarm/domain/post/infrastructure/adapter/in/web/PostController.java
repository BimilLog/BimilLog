package jaeik.growfarm.domain.post.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostNoticeUseCase;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
@RequestMapping("/api/posts")
public class PostController {

    private final PostCommandUseCase postCommandUseCase;
    private final PostQueryUseCase postQueryUseCase;
    private final PostInteractionUseCase postInteractionUseCase;
    private final PostNoticeUseCase postNoticeUseCase;

    // Query Endpoints
    /**
     * <h3>게시판 목록 조회 API</h3>
     * <p>최신순으로 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return 게시글 목록 페이지 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping
    public ResponseEntity<Page<SimplePostResDTO>> getBoard(Pageable pageable) {
        Page<SimplePostResDTO> postList = postQueryUseCase.getBoard(pageable);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>게시글 검색 API</h3>
     * <p>검색 유형(type)과 검색어(query)를 통해 게시글을 검색하고 최신순으로 페이지네이션합니다.</p>
     *
     * @param type     검색 유형 (예: title, content, writer)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 목록 페이지 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostResDTO>> searchPost(@RequestParam String type,
                                                               @RequestParam String query,
                                                               Pageable pageable) {
        Page<SimplePostResDTO> postList = postQueryUseCase.searchPost(type, query, pageable);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>게시글 상세 조회 API</h3>
     * <p>게시글 ID를 통해 게시글 상세 정보를 조회하고 조회수를 증가시킵니다.</p>
     *
     * @param postId      조회할 게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보 (Optional, 추천 여부 확인용)
     * @return 게시글 상세 정보 DTO (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/{postId}")
    public ResponseEntity<FullPostResDTO> getPost(@PathVariable Long postId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        postInteractionUseCase.incrementViewCount(postId); // 조회수 증가 (Command)
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        FullPostResDTO fullPostResDTO = postQueryUseCase.getPost(postId, userId); // 게시글 조회 (Query)
        return ResponseEntity.ok(fullPostResDTO);
    }

    // Command Endpoints
    /**
     * <h3>게시글 작성 API</h3>
     * <p>새로운 게시글을 작성하고 저장합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 생성된 게시글의 URI를 포함한 응답 (201 Created)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping
    public ResponseEntity<Void> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody @Valid PostReqDTO postReqDTO) {
        Long postId = postCommandUseCase.writePost(userDetails.getUserId(), postReqDTO);
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
        postCommandUseCase.updatePost(userDetails.getUserId(), postId, postReqDTO);
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
        postNoticeUseCase.setPostAsNotice(postId);
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
        postNoticeUseCase.unsetPostAsNotice(postId);
        return ResponseEntity.ok().build();
    }
}

