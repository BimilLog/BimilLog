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
 * <h2>PostCommandController</h2>
 * <p>
 * Post 도메인의 명령(CUD) 관련 REST API 엔드포인트를 제공하는 웹 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 외부 웹 요청을 도메인 비즈니스 로직으로 연결하며,
 * CQRS 패턴에 따라 명령 전용 API를 분리하여 책임을 명확화합니다.
 * </p>
 * <p>
 * 프론트엔드에서 게시글 작성, 수정, 삭제, 좋아요 토글 요청 시 호출되어 비즈니스 로직을 실행하고,
 * 로그인 사용자와 익명 사용자 모두 게시글 작성이 가능하도록 유연한 인증 처리를 제공합니다.
 * </p>
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
     * <p>로그인 사용자는 자동으로 작성자 정보가 설정되고, 비로그인 사용자는 비밀번호를 통해 익명으로 작성할 수 있습니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보 (Optional - 비로그인 사용자 허용)
     * @param postReqDTO  게시글 작성 요청 DTO
     * @return 생성된 게시글의 URI를 포함한 응답 (201 Created)
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
        postCommandUseCase.updatePost(userDetails.getUserId(), postId, postReqDTO.getTitle(), postReqDTO.getContent());
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

