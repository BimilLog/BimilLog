package jaeik.growfarm.domain.post.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.post.application.port.in.PostCommandUseCase;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostCommandUseCase postCommandUseCase;
    private final PostQueryUseCase postQueryUseCase;

    // Query Endpoints
    @GetMapping
    public ResponseEntity<Page<SimplePostResDTO>> getBoard(Pageable pageable) {
        Page<SimplePostResDTO> postList = postQueryUseCase.getBoard(pageable);
        return ResponseEntity.ok(postList);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SimplePostResDTO>> searchPost(@RequestParam String type,
                                                               @RequestParam String query,
                                                               Pageable pageable) {
        Page<SimplePostResDTO> postList = postQueryUseCase.searchPost(type, query, pageable);
        return ResponseEntity.ok(postList);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<FullPostResDTO> getPost(@PathVariable Long postId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        postCommandUseCase.incrementViewCount(postId); // 조회수 증가 (Command)
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        FullPostResDTO fullPostResDTO = postQueryUseCase.getPost(postId, userId); // 게시글 조회 (Query)
        return ResponseEntity.ok(fullPostResDTO);
    }

    // Command Endpoints
    @PostMapping
    public ResponseEntity<Void> writePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody @Valid PostReqDTO postReqDTO) {
        Long postId = postCommandUseCase.writePost(userDetails.getUserId(), postReqDTO);
        return ResponseEntity.created(URI.create("/api/posts/" + postId)).build();
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody @Valid PostReqDTO postReqDTO) {
        postCommandUseCase.updatePost(userDetails.getUserId(), postId, postReqDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        postCommandUseCase.deletePost(userDetails.getUserId(), postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        postCommandUseCase.likePost(userDetails.getUserId(), postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setPostAsNotice(@PathVariable Long postId) {
        postCommandUseCase.setPostAsNotice(postId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/notice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unsetPostAsNotice(@PathVariable Long postId) {
        postCommandUseCase.unsetPostAsNotice(postId);
        return ResponseEntity.ok().build();
    }
}

