package jaeik.growfarm.controller.post;

import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.post.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 관리 컨트롤러</h2>
 * <p>
 * 게시글의 CRUD 및 좋아요 관리 기능을 담당
 * </p>
 * 
 * @author Jaeik
 * @version 1.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/manage")
public class PostManageController {

    private final PostService postService;

    /**
     * <h3>게시글 작성 API</h3>
     *
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     * 
     * @since 1.1.0
     * @author Jaeik
     * @param postReqDTO  게시글 작성 요청 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성된 게시글 정보
     */
    @PostMapping("/write")
    public ResponseEntity<PostDTO> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostReqDTO postReqDTO) {
        PostDTO postDTO = postService.writePost(userDetails, postReqDTO);
        return ResponseEntity.ok(postDTO);
    }

    /**
     * <h3>게시글 수정 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     수정할 게시글 정보
     * @return 수정된 게시글 정보
     * @author Jaeik
     * @since 1.1.0
     */
    @PostMapping("/update")
    public ResponseEntity<String> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostDTO postDTO) {
        postService.updatePost(userDetails, postDTO);
        return ResponseEntity.ok("게시글 수정 완료");
    }

    /**
     * <h3>게시글 삭제 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.1.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     삭제할 게시글 정보
     * @return 삭제 성공 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostDTO postDTO) {
        postService.deletePost(userDetails, postDTO);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    /**
     * <h3>게시글 추천/추천 취소 API</h3>
     *
     * <p>
     * 게시글에 추천을 하거나 취소한다.
     * </p>
     * 
     * @since 1.0.21
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postDTO     추천/추천 취소할 게시글 정보
     * @return 좋아요 처리 결과 메시지
     */
    @PostMapping("/like")
    public ResponseEntity<String> likePost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostDTO postDTO) {
        postService.likePost(postDTO, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
    }
}