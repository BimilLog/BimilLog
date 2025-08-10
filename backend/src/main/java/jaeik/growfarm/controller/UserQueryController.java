package jaeik.growfarm.controller;

import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 조회 컨트롤러</h2>
 * <p>
 * 사용자 관련 조회 작업만 담당하는 컨트롤러
 * SRP: 사용자 데이터 조회 기능만 담당 (Query in CQRS)
 * </p>
 * <p>
 * 담당 기능:
 * - 작성 게시글/댓글 목록 조회
 * - 좋아요한 게시글/댓글 목록 조회  
 * - 닉네임 중복 확인
 * - 설정 조회
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserQueryController {

    private final UserService userService;

    /**
     * <h3>사용자 작성 게시글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자의 작성 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 게시글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SimplePostResDTO>> getPostList(@RequestParam int page,
                                                              @RequestParam int size,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostResDTO> postList = userService.getPostList(page, size, userDetails);
        return ResponseEntity.ok(postList);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자의 작성 댓글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/comments")
    public ResponseEntity<Page<SimpleCommentDTO>> getCommentList(@RequestParam int page,
                                                                 @RequestParam int size,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimpleCommentDTO> commentList = userService.getCommentList(page, size, userDetails);
        return ResponseEntity.ok(commentList);
    }

    /**
     * <h3>사용자가 추천한 게시글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자가 좋아요한 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 게시글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/likeposts")
    public ResponseEntity<Page<SimplePostResDTO>> getLikedPosts(@RequestParam int page,
                                                                @RequestParam int size,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostResDTO> likedPosts = userService.getLikedPosts(page, size, userDetails);
        return ResponseEntity.ok(likedPosts);
    }

    /**
     * <h3>사용자가 추천한 댓글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자가 좋아요한 댓글 목록을 페이지네이션으로 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/likecomments")
    public ResponseEntity<Page<SimpleCommentDTO>> getLikedComments(@RequestParam int page,
                                                                   @RequestParam int size,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimpleCommentDTO> likedComments = userService.getLikedComments(page, size, userDetails);
        return ResponseEntity.ok(likedComments);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     *
     * <p>
     * 닉네임이 이미 존재하는지 확인한다.
     * </p>
     *
     * @param userName 확인할 닉네임
     * @return 닉네임 중복 여부
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String userName) {
        boolean isAvailable = userService.isUserNameAvailable(userName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>사용자 설정 조회 API</h3>
     *
     * <p>
     * 사용자의 현재 설정 정보를 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 설정 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SettingDTO settingDTO = userService.getSetting(userDetails);
        return ResponseEntity.ok(settingDTO);
    }
}