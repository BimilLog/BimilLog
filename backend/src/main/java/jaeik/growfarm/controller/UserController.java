package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.user.FarmNameReqDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.UserService;
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
 * 농장 이름 변경, 건의하기, 카카오 친구 목록
 * </p>
 * <p>
 * 설정 조회 및 수정
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * <h3>사용자 작성 게시글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자의 작성 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 게시글 목록 페이지
     */
    @GetMapping("/mypage/posts")
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
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     */
    @GetMapping("/mypage/comments")
    public ResponseEntity<Page<CommentDTO>> getCommentList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<CommentDTO> commentList = userService.getCommentList(page, size, userDetails);
        return ResponseEntity.ok(commentList);
    }

    /**
     * <h3>사용자가 추천한 게시글 목록 조회 API</h3>
     *
     * <p>
     * 해당 사용자가 좋아요한 게시글 목록을 페이지네이션으로 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 게시글 목록 페이지
     */
    @GetMapping("/mypage/likedposts")
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
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 댓글 목록 페이지
     */
    @GetMapping("/mypage/likedcomments")
    public ResponseEntity<Page<CommentDTO>> getLikedComments(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<CommentDTO> likedComments = userService.getLikedComments(page, size, userDetails);
        return ResponseEntity.ok(likedComments);
    }

    /**
     * <h3>농장 이름 변경 API</h3>
     *
     * <p>
     * 사용자의 농장 이름을 변경한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param farmNameReqDTO 농장 이름 요청 DTO
     * @param userDetails    현재 로그인한 사용자 정보
     * @return 변경 성공 메시지
     */
    @PostMapping("/mypage/updatefarm")
    public ResponseEntity<String> updateFarmName(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid FarmNameReqDTO farmNameReqDTO) {
        userService.updateFarmName(farmNameReqDTO.getFarmName(), userDetails);
        return ResponseEntity.ok("농장 이름이 변경되었습니다.");
    }

    /**
     * <h3>건의하기 API</h3>
     *
     * <p>
     * 사용자의 건의사항을 접수한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param reportDTO 건의 내용 DTO
     * @return 건의 접수 성공 메시지
     */
    @PostMapping("/suggestion")
    public ResponseEntity<Void> suggestion(@RequestBody ReportDTO reportDTO) {
        userService.suggestion(reportDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>카카오 친구 목록 조회 API</h3>
     *
     * <p>
     * 카카오 API를 통해 친구 목록을 가져온다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param offset      페이지 오프셋
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 카카오 친구 목록
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
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 설정 정보
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getClientDTO().getUserId();
        SettingDTO settingDTO = userService.getSetting(userId);
        return ResponseEntity.ok(settingDTO);
    }

    /**
     * <h3>사용자 설정 수정 API</h3>
     *
     * <p>
     * 사용자의 알림 설정을 수정한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param settingDTO  설정 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 설정 수정 성공 메시지
     */
    @PostMapping("/setting")
    public ResponseEntity<SettingDTO> updateSetting(@RequestBody SettingDTO settingDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getClientDTO().getUserId();
        userService.updateSetting(settingDTO, userId);
        SettingDTO newSettingDTO = userService.getSetting(userId);
        return ResponseEntity.ok(newSettingDTO);
    }
}
