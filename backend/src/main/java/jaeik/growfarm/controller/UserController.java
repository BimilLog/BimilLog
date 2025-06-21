package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "사용자", description = "사용자 관련 API")
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
     * @param page        페이지 번호
     * @param size        페이지 사이즈
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    @Operation(summary = "작성 게시글 목록 조회", description = "현재 로그인한 사용자가 작성한 게시글 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @GetMapping("/posts")
    public ResponseEntity<Page<SimplePostDTO>> getPostList(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "작성 댓글 목록 조회", description = "현재 로그인한 사용자가 작성한 댓글 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @GetMapping("/comments")
    public ResponseEntity<Page<SimpleCommentDTO>> getCommentList(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "추천한 게시글 목록 조회", description = "현재 로그인한 사용자가 추천한 게시글 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 게시글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @GetMapping("/likeposts")
    public ResponseEntity<Page<SimplePostDTO>> getLikedPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "추천한 댓글 목록 조회", description = "현재 로그인한 사용자가 추천한 댓글 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @GetMapping("/likecomments")
    public ResponseEntity<Page<SimpleCommentDTO>> getLikedComments(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam int page,
            @Parameter(description = "페이지 크기") @RequestParam int size,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SimpleCommentDTO> likedComments = userService.getLikedComments(page, size, userDetails);
        return ResponseEntity.ok(likedComments);
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
    @Operation(summary = "닉네임 변경", description = "현재 로그인한 사용자의 닉네임을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 닉네임입니다."),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "변경할 닉네임 정보") @RequestBody @Valid UserNameDTO userNameDTO) {
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
    @Operation(summary = "건의사항 제출", description = "사용자의 건의사항을 접수합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 접수 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @PostMapping("/suggestion")
    public ResponseEntity<String> suggestion(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "건의사항 내용") @RequestBody ReportDTO reportDTO) {
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
    @Operation(summary = "카카오 친구 목록 조회", description = "카카오 API를 통해 친구 목록을 가져옵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "카카오 친구 추가 동의를 해야 합니다."),
    })
    @PostMapping("/friendlist")
    public ResponseEntity<KakaoFriendListDTO> getFriendList(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 오프셋") @RequestParam int offset) {
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
    @Operation(summary = "사용자 설정 조회", description = "현재 로그인한 사용자의 설정 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "404", description = "설정 정보를 찾을 수 없습니다.")
    })
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @Operation(summary = "사용자 설정 수정", description = "현재 로그인한 사용자의 알림 설정을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 수정 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "404", description = "설정 정보를 찾을 수 없습니다.")
    })
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(
            @Parameter(description = "수정할 설정 정보") @RequestBody SettingDTO settingDTO,
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateSetting(settingDTO, userDetails);
        return ResponseEntity.ok("설정 수정 완료");
    }
}
