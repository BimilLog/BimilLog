package jaeik.growfarm.controller.post;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.PostLikeRequestDTO;
import jaeik.growfarm.dto.post.PostReqDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.post.command.PostCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>게시글 관리 컨트롤러</h2>
 * <p>
 * 게시글의 CRUD 및 좋아요 관리 기능을 담당
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post/manage")
public class PostManageController {

    private final PostCommandService postCommandService;

    /**
     * <h3>공지사항 설정 API</h3>
     *
     * @param postId 공지사항으로 설정할 게시글 ID
     * @return 설정 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> setPostAsNotice(@PathVariable Long postId) {
        postCommandService.setPostAsNotice(postId);
        return ResponseEntity.ok("게시글을 공지사항으로 설정했습니다.");
    }

    /**
     * <h3>공지사항 해제 API</h3>
     *
     * @param postId 공지사항을 해제할 게시글 ID
     * @return 해제 완료 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    @DeleteMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unsetPostAsNotice(@PathVariable Long postId) {
        postCommandService.unsetPostAsNotice(postId);
        return ResponseEntity.ok("게시글의 공지사항을 해제했습니다.");
    }

    /**
     * <h3>게시글 작성 API</h3>
     *
     * <p>
     * 새로운 게시글을 작성하고 저장한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param postReqDTO  게시글 작성 요청 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성된 게시글 정보
     */
    @PostMapping("/write")
    public ResponseEntity<FullPostResDTO> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @RequestBody @Valid PostReqDTO postReqDTO) {
        FullPostResDTO fullPostResDTO = postCommandService.writePost(userDetails, postReqDTO);
        return ResponseEntity.ok(fullPostResDTO);
    }

    /**
     * <h3>게시글 수정 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 수정할 수 있다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO     수정할 게시글 정보
     * @return 수정된 게시글 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/update")
    public ResponseEntity<String> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostReqDTO postReqDTO) {
        postCommandService.updatePost(userDetails, postReqDTO);
        return ResponseEntity.ok("게시글 수정 완료");
    }

    /**
     * <h3>게시글 삭제 API</h3>
     *
     * <p>
     * 게시글 작성자만 게시글을 삭제할 수 있다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param postReqDTO     삭제할 게시글 정보
     * @return 삭제 성공 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostReqDTO postReqDTO) {
        postCommandService.deletePost(userDetails, postReqDTO);
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
     * @param likeRequestDTO     추천/추천 취소할 게시글 정보
     * @return 좋아요 처리 결과 메시지
     */
    @PostMapping("/like")
    public ResponseEntity<String> likePost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PostLikeRequestDTO likeRequestDTO) {
        postCommandService.likePost(likeRequestDTO, userDetails);
        return ResponseEntity.ok("추천 처리 완료");
    }
}