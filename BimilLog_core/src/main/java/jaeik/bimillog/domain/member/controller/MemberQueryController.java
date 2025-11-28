package jaeik.bimillog.domain.member.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.dto.KakaoFriendsDTO;
import jaeik.bimillog.domain.member.dto.SettingDTO;
import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.service.MemberFriendService;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.log.Log;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>멤버 조회 컨트롤러</h2>
 * <p>닉네임 중복 조회, 설정조회, 카카오친구 목록 조회, 모든 멤버 조회, 멤버 검색</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberQueryController {
    private final MemberQueryService memberQueryService;
    private final MemberFriendService memberFriendService;

    /**
     * <h3>닉네임 중복 확인 API</h3>
     * <p>사용자의 닉네임이 이미 사용 중인지 확인하는 요청을 처리</p>
     *
     * @param memberName 닉네임
     * @return 닉네임 사용 가능 여부
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String memberName) {
        boolean isAvailable = !memberQueryService.existsByMemberName(memberName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>사용자 설정 조회 API</h3>
     *
     * @param userDetails 사용자 인증 정보 (JWT에서 settingId 포함)
     * @return 사용자 설정 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Setting setting = memberQueryService.findBySettingId(userDetails.getSettingId());
        return ResponseEntity.ok(SettingDTO.fromSetting(setting));
    }

    /**
     * <h3>카카오 친구 목록 조회 API</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고 비밀로그 가입 여부를 포함하여 반환합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Mono<ResponseEntity<KakaoFriendsDTO>> 카카오 친구 목록 (비밀로그 가입 여부 포함)
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/friendlist")
    public ResponseEntity<KakaoFriendsDTO> getKakaoFriendList(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                              Pageable pageable) {
        KakaoFriendsDTO friendsResponse = memberFriendService.getKakaoFriendList(
                userDetails.getMemberId(),
                userDetails.getSocialProvider(),
                pageable.getOffset(),
                pageable.toLimit()
        );

        return ResponseEntity.ok(friendsResponse);
    }

    /**
     * <h3>멤버 목록 조회 API</h3>
     *
     * @return Page<SimpleMemberDTO> 멤버 목록
     * @since 2.1.0
     * @author Jaeik
     */
    @GetMapping("/all")
    public ResponseEntity<Page<SimpleMemberDTO>> getAllMembers(Pageable pageable) {
        Page<SimpleMemberDTO> members = memberQueryService.findAllMembers(pageable)
                .map(SimpleMemberDTO::fromMember);
        return ResponseEntity.ok(members);
    }

    /**
     * <h3>멤버 검색 API</h3>
     * @param pageable 페이지
     * @return Page<String> 멤버이름
     * @since 2.1.0
     * @author Jaeik
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SimpleMemberDTO>> searchMembers(@RequestParam @NotBlank(message = "검색어는 필수입니다") String query, Pageable pageable) {
        Page<SimpleMemberDTO> members = memberQueryService.searchMembers(query.trim(), pageable);
        return ResponseEntity.ok(members);
    }
}
