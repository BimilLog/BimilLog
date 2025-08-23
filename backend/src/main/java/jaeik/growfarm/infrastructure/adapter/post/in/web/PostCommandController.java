package jaeik.growfarm.infrastructure.adapter.post.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/post")
public class PostCommandController {

    private final PostCommandUseCase postCommandUseCase;
    private final PostInteractionUseCase postInteractionUseCase;

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
}

