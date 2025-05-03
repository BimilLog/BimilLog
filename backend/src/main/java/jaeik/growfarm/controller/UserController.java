package jaeik.growfarm.controller;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/*
 * 유저 관련 API
 * 유저의 작성 게시글 목록 조회
 * 유저의 작성 댓글 목록 조회
 * 유저가 추천한 게시글 목록 조회
 * 유저가 추천한 댓글 목록 조회
 * 농장 이름 변경
 * 건의하기
 * 카카오 친구 목록 가져오기
 * 설정 조회
 * 설정 수정
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    /*
     * 유저의 작성 게시글 목록 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<Page<SimplePostDTO>> 유저의 작성 게시글 목록
     * 수정일 : 2025-05-03
     */
    @GetMapping("/mypage/posts")
    public ResponseEntity<Page<SimplePostDTO>> getPostList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<SimplePostDTO> postList = userService.getPostList(page, size, userDetails);
        return ResponseEntity.ok(postList);
    }

    /*
     * 유저의 작성 댓글 목록 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<Page<CommentDTO>> 유저의 작성 댓글 목록
     * 수정일 : 2025-05-03
     */
    @GetMapping("/mypage/comments")
    public ResponseEntity<Page<CommentDTO>> getCommentList(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<CommentDTO> commentList = userService.getCommentList(page, size, userDetails);
        return ResponseEntity.ok(commentList);
    }

    /*
     * 유저가 추천한 게시글 목록 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<Page<SimplePostDTO>> 유저가 추천한 게시글 목록
     * 수정일 : 2025-05-03
     */
    @GetMapping("/mypage/likedposts")
    public ResponseEntity<Page<SimplePostDTO>> getLikedPosts(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimplePostDTO> likedPosts = userService.getLikedPosts(page, size, userDetails);
        return ResponseEntity.ok(likedPosts);
    }

    /*
     * 유저가 추천한 댓글 목록 조회 API
     * param int page: 페이지 번호
     * param int size: 페이지 사이즈
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<Page<CommentDTO>> 유저가 추천한 댓글 목록
     * 수정일 : 2025-05-03
     */
    @GetMapping("/mypage/likedcomments")
    public ResponseEntity<Page<CommentDTO>> getLikedComments(@RequestParam int page,
            @RequestParam int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<CommentDTO> likedComments = userService.getLikedComments(page, size, userDetails);
        return ResponseEntity.ok(likedComments);
    }

    /*
     * 농장 이름 변경 API
     * param String farmName: 변경할 농장 이름
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<String> 농장 이름 변경 완료 메시지
     * 수정일 : 2025-05-03
     */
    @GetMapping("/mypage/updatefarm")
    public ResponseEntity<String> updateFarmName(@RequestParam String farmName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateFarmName(farmName, userDetails);
        return ResponseEntity.ok("농장 이름이 변경되었습니다.");
    }

    /*
     * 건의하기 API
     * param ReportDTO reportDTO: 건의 내용 DTO
     * return: ResponseEntity<String> 건의 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/suggestion")
    public ResponseEntity<String> suggestion(@RequestBody ReportDTO reportDTO) {
        userService.suggestion(reportDTO);
        return ResponseEntity.ok("건의가 완료되었습니다.");
    }

    /*
     * 카카오 친구 목록 가져오기 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * param int offset: 페이지 번호
     * return: ResponseEntity<KakaoFriendListDTO> 카카오 친구 목록
     * 수정일 : 2025-05-03
     */
    @PostMapping("/friendlist")
    public ResponseEntity<KakaoFriendListDTO> getFriendList(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int offset) {
        return ResponseEntity.ok(userService.getFriendList(userDetails, offset));
    }

    /*
     * 설정 조회 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<SettingDTO> 설정 DTO
     * 수정일 : 2025-05-03
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserDTO().getUserId();
        SettingDTO settingDTO = userService.getSetting(userId);
        return ResponseEntity.ok(settingDTO);
    }

    /*
     * 설정 수정 API
     * param SettingDTO settingDTO: 설정 DTO
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<SettingDTO> 수정된 설정 DTO
     * 수정일 : 2025-05-03
     */
    @PostMapping("/setting")
    public ResponseEntity<SettingDTO> updateSetting(@RequestBody SettingDTO settingDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserDTO().getUserId();
        userService.updateSetting(settingDTO, userId);
        SettingDTO newSettingDTO = userService.getSetting(userId);
        return ResponseEntity.ok(newSettingDTO);
    }
}
