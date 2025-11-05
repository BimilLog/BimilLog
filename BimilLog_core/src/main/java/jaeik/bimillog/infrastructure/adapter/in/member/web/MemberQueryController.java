package jaeik.bimillog.infrastructure.adapter.in.member.web;

import jaeik.bimillog.domain.member.application.port.in.MemberFriendUseCase;
import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.MemberSearchDTO;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.in.member.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.adapter.out.api.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>사용자 조회 컨트롤러</h2>
 * <p>사용자 관련 조회 REST API 요청을 처리하는 인바운드 어댑터입니다.</p>
 * <p>닉네임 중복 확인, 사용자 설정 조회, 카카오 친구 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberQueryController {

    private final MemberQueryUseCase memberQueryUseCase;
    private final MemberFriendUseCase memberFriendUseCase;

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
        boolean isAvailable = !memberQueryUseCase.existsByMemberName(memberName);
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
        Setting setting = memberQueryUseCase.findBySettingId(userDetails.getSettingId());
        return ResponseEntity.ok(SettingDTO.fromSetting(setting));
    }

    /**
     * <h3>카카오 친구 목록 조회 API</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고 비밀로그 가입 여부를 포함하여 반환합니다.</p>
     *
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Mono<ResponseEntity<KakaoFriendsDTO>> 카카오 친구 목록 (비밀로그 가입 여부 포함)
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/friendlist")
    public ResponseEntity<KakaoFriendsDTO> getKakaoFriendList(@RequestParam(defaultValue = "0") Integer offset,
                                                               @RequestParam(defaultValue = "10") Integer limit,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        KakaoFriends friendsResponseVO = memberFriendUseCase.getKakaoFriendList(
                userDetails.getMemberId(),
                userDetails.getTokenId(),
                userDetails.getSocialProvider(),
                offset,
                limit
        );

        KakaoFriendsDTO friendsResponse = KakaoFriendsDTO.fromVO(friendsResponseVO);
        return ResponseEntity.ok(friendsResponse);
    }

    /**
     * <h3>사용자 검색 API</h3>
     * <p>검색어로 사용자명을 검색합니다.</p>
     * <p>검색 전략: 4글자 이상이면 접두사 검색, 그 외에는 부분 검색</p>
     *
     * @param searchDTO 검색 요청 DTO (query 포함)
     * @param pageable  페이징 정보
     * @return Page<String> 검색된 사용자명 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/search")
    public ResponseEntity<Page<String>> searchMembers(
            @Valid @ModelAttribute MemberSearchDTO searchDTO,
            Pageable pageable) {

        Page<String> memberNames = memberQueryUseCase.searchMembers(
                searchDTO.getTrimmedQuery(),
                pageable
        );

        return ResponseEntity.ok(memberNames);
    }
}