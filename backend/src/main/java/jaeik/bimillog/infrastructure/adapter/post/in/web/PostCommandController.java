package jaeik.bimillog.infrastructure.adapter.post.in.web;

import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.infrastructure.adapter.post.dto.PostReqDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * <h2>게시글 명령 컨트롤러</h2>
 * <p>게시글 도메인의 명령 관련 REST API를 제공하는 웹 어댑터입니다.</p>
 * <p>게시글 작성, 수정, 삭제, 추천 API</p>
 * <p>익명/회원 사용자 인증 처리</p>
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
     * <p>로그인/익명 사용자 모두 작성 가능, 익명 시 비밀번호 설정</p>
     *
     * @param userDetails 현재 로그인 사용자 정보 (익명 사용자는 null)
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 생성된 게시글 URI (201 Created)
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping
    public ResponseEntity<Void> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody @Valid PostReqDTO postReqDTO) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        Integer passwordInt = parsePassword(postReqDTO.getPassword());
        Long postId = postCommandUseCase.writePost(userId, postReqDTO.getTitle(), postReqDTO.getContent(), passwordInt);
        return ResponseEntity.created(URI.create("/api/posts/" + postId)).build();
    }

    /**
     * <h3>게시글 수정 API</h3>
     * <p>게시글 작성자만 수정 가능합니다.</p>
     *
     * @param postId      수정할 게시글 ID
     * @param userDetails 현재 로그인 사용자 정보
     * @param postReqDTO  수정할 게시글 정보 DTO
     * @return 성공 응답 (200 OK)
     * @author Jaeik
     * @since 2.0.0
     */
    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody @Valid PostReqDTO postReqDTO) {
        postCommandUseCase.updatePost(userDetails.getUserId(), postId, postReqDTO.getTitle(), postReqDTO.getContent());
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>게시글 삭제 API</h3>
     * <p>게시글 작성자만 삭제 가능합니다.</p>
     *
     * @param postId      삭제할 게시글 ID
     * @param userDetails 현재 로그인 사용자 정보
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
     * <h3>게시글 추천 토글 API</h3>
     * <p>게시글 추천/추천 취소를 토글 방식으로 처리합니다.</p>
     *
     * @param postId      추천 토글할 게시글 ID
     * @param userDetails 현재 로그인 사용자 정보
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
     * <h3>비밀번호 문자열을 정수로 변환</h3>
     *
     * @param password 변환할 비밀번호 문자열
     * @return Integer 비밀번호 (null 가능)
     * @author jaeik
     * @since 2.0.0
     */
    private Integer parsePassword(String password) {
        if (password != null && !password.trim().isEmpty()) {
            return Integer.parseInt(password);
        }
        return null;
    }
}

