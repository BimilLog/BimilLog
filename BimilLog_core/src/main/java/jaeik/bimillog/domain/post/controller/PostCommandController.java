package jaeik.bimillog.domain.post.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.post.dto.PostCreateDTO;
import jaeik.bimillog.domain.post.dto.PostDeleteDTO;
import jaeik.bimillog.domain.post.dto.PostUpdateDTO;
import jaeik.bimillog.domain.post.service.PostCommandService;
import jaeik.bimillog.domain.post.service.PostInteractionService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.log.Log;
import jaeik.bimillog.infrastructure.log.Log.LogLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * <h2>게시글 명령 컨트롤러</h2>
 * <p>게시글 도메인의 명령 처리 컨트롤러</p>
 * <p>게시글 작성, 수정, 삭제, 추천 API</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = LogLevel.INFO,
        logExecutionTime = true,
        excludeParams = {"password"},
        logParams = false)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostCommandController {
    private final PostCommandService postCommandService;
    private final PostInteractionService postInteractionService;

    /**
     * <h3>게시글 작성 API</h3>
     * <p>새로운 게시글을 작성하고 저장합니다.</p>
     * <p>로그인/익명 사용자 모두 작성 가능, 익명 시 비밀번호 설정</p>
     *
     * @param userDetails   현재 로그인 사용자 정보 (익명 사용자는 null)
     * @param postCreateDTO 게시글 작성 요청 DTO
     * @return 생성된 게시글 URI (201 Created)
     */
    @PostMapping
    public ResponseEntity<Void> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody @Valid PostCreateDTO postCreateDTO) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        Long postId = postCommandService.writePost(memberId, postCreateDTO.getTitle(), postCreateDTO.getContent(), postCreateDTO.getPassword());
        return ResponseEntity.created(URI.create("/post/" + postId)).build();
    }

    /**
     * <h3>게시글 수정 API</h3>
     * <p>게시글 작성자만 수정 가능합니다.</p>
     *
     * @param postId        수정할 게시글 ID
     * @param userDetails   현재 로그인 사용자 정보
     * @param postUpdateDTO 수정할 게시글 정보 DTO
     * @return 성공 응답 (200 OK)
     */
    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody @Valid PostUpdateDTO postUpdateDTO) {
        Long memberId = (userDetails != null) ? userDetails.getMemberId() : null;
        postCommandService.updatePost(memberId, postId, postUpdateDTO.getTitle(), postUpdateDTO.getContent(), postUpdateDTO.getPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>게시글 삭제 API</h3>
     * <p>게시글 작성자만 삭제 가능합니다.</p>
     *
     * @param postId        삭제할 게시글 ID
     * @param userDetails   현재 로그인 사용자 정보
     * @param postDeleteDTO 삭제할 게시글 정보
     * @return 성공 응답 (204 No Content)
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody(required = false) @Valid PostDeleteDTO postDeleteDTO) {

        Long memberId = (userDetails != null) ? userDetails.getMemberId() : null;
        Integer password = (postDeleteDTO != null) ? postDeleteDTO.getPassword() : null;

        if (memberId == null && password == null) {
            throw new CustomException(ErrorCode.POST_BLANK_PASSWORD);
        }

        postCommandService.deletePost(memberId, postId, password);
        return ResponseEntity.noContent().build();
    }

    /**
     * <h3>게시글 추천/추천 취소 API</h3>
     * <p>게시글 추천/추천 취소를 토글 방식으로 처리합니다.</p>
     *
     * @param postId      추천 토글할 게시글 ID
     * @param userDetails 현재 로그인 사용자 정보
     * @return 성공 응답 (200 OK)
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        postInteractionService.likePost(userDetails.getMemberId(), postId);
        return ResponseEntity.ok().build();
    }
}
