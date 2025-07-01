package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 관련 컨트롤러</h2>
 * <p>
 * 사용자의 작성 게시글/댓글 목록 조회
 * </p>
 * <p>
 * 사용자가 좋아요한 게시글/댓글 목록 조회
 * </p>
 * <p>
 * 닉네임 변경, 건의하기, 카카오 친구 목록 불러오기
 * </p>
 * <p>
 * 설정 조회 및 수정
 * </p>
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

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
     * @since 1.0.0
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SimplePostDTO>> getPostList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostDTO> postList = userService.getPostList(page, size, userDetails);
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
     * @since 1.0.0
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
     * @since 1.0.0
     */
    @GetMapping("/likeposts")
    public ResponseEntity<Page<SimplePostDTO>> getLikedPosts(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostDTO> likedPosts = userService.getLikedPosts(page, size, userDetails);
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
     * @since 1.0.0
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
     * 닉네임이 이미 존재하지는 확인한다.
     * </p>
     *
     * @return 닉네임 중복 여부
     * @author Jaeik
     * @since 1.0.0
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String userName) {
        boolean isAvailable = userService.isUserNameAvailable(userName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>닉네임 변경 API</h3>
     *
     * <p>
     * 닉네임 변경한다.
     * </p>
     *
     * @param userNameDTO 닉네임 변경 요청 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 변경 성공 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserNameDTO userNameDTO) {
        userService.updateUserName(userNameDTO.getUserName(), userDetails);
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    /**
     * <h3>건의하기 API</h3>
     *
     * <p>
     * 사용자의 건의사항을 접수한다.
     * </p>
     *
     * @param reportDTO 건의 내용 DTO
     * @return 건의 접수 성공 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("/suggestion")
    public ResponseEntity<String> suggestion(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReportDTO reportDTO) {
        userService.suggestion(userDetails, reportDTO);
        return ResponseEntity.ok("건의가 접수되었습니다.");
    }

    /**
     * <h3>카카오 친구 목록 조회 API</h3>
     *
     * <p>
     * 카카오 API를 통해 친구 목록을 가져온다.
     * </p>
     *
     * @param offset      페이지 오프셋
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 카카오 친구 목록
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("/friendlist")
    public ResponseEntity<KakaoFriendListDTO> getFriendList(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int offset) {
        return ResponseEntity.ok(userService.getFriendList(userDetails, offset));
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
     * @since 1.0.0
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SettingDTO settingDTO = userService.getSetting(userDetails);
        return ResponseEntity.ok(settingDTO);
    }

    /**
     * <h3>사용자 설정 수정 API</h3>
     *
     * <p>
     * 사용자의 알림 설정을 수정한다.
     * </p>
     *
     * @param settingDTO  설정 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 설정 수정 성공 메시지
     * @author Jaeik
     * @since 1.0.0
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody SettingDTO settingDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateSetting(settingDTO, userDetails);
        return ResponseEntity.ok("설정 수정 완료");
    }
}
