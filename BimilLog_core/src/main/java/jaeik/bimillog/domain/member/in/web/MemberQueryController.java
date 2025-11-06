package jaeik.bimillog.domain.member.in.web;

import jaeik.bimillog.domain.member.application.port.in.MemberFriendUseCase;
import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.in.dto.MemberSearchDTO;
import jaeik.bimillog.domain.member.in.dto.SettingDTO;
import jaeik.bimillog.domain.member.in.dto.SimpleMemberDTO;
import jaeik.bimillog.infrastructure.out.api.dto.KakaoFriendsDTO;
import jaeik.bimillog.domain.auth.out.CustomUserDetails;
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
 * <h2>?�용??조회 컨트롤러</h2>
 * <p>?�용??관??조회 REST API ?�청??처리?�는 ?�바?�드 ?�댑?�입?�다.</p>
 * <p>?�네??중복 ?�인, ?�용???�정 조회, 카카??친구 목록 조회</p>
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
     * <h3>?�네??중복 ?�인 API</h3>
     * <p>?�용?�의 ?�네?�이 ?��? ?�용 중인지 ?�인?�는 ?�청??처리</p>
     *
     * @param memberName ?�네??
     * @return ?�네???�용 가???��?
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String memberName) {
        boolean isAvailable = !memberQueryUseCase.existsByMemberName(memberName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>?�용???�정 조회 API</h3>
     *
     * @param userDetails ?�용???�증 ?�보 (JWT?�서 settingId ?�함)
     * @return ?�용???�정 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Setting setting = memberQueryUseCase.findBySettingId(userDetails.getSettingId());
        return ResponseEntity.ok(SettingDTO.fromSetting(setting));
    }

    /**
     * <h3>카카??친구 목록 조회 API</h3>
     * <p>?�재 로그?�한 ?�용?�의 카카??친구 목록??조회?�고 비�?로그 가???��?�??�함?�여 반환?�니??</p>
     *
     * @param offset      조회 ?�작 ?�치 (기본�? 0)
     * @param limit       조회??친구 ??(기본�? 10, 최�?: 100)
     * @param userDetails ?�재 로그?�한 ?�용???�보
     * @return Mono<ResponseEntity<KakaoFriendsDTO>> 카카??친구 목록 (비�?로그 가???��? ?�함)
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
     * <h3>?�체 ?�원 목록 조회 API</h3>
     * <p>방문 ?�이지?�서 ?�용???�체 ?�원 목록???�이지 ?�태�?반환?�니??</p>
     *
     * @param page ?�이지 번호 (0부???�작)
     * @param size ?�이지 ?�기
     * @return Page<SimpleMemberDTO> ?�원 목록 ?�이지
     * @since 2.1.0
     */
    @GetMapping("/all")
    public ResponseEntity<Page<SimpleMemberDTO>> getAllMembers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimpleMemberDTO> members = memberQueryUseCase.findAllMembers(pageable)
                .map(SimpleMemberDTO::fromMember);
        return ResponseEntity.ok(members);
    }

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
