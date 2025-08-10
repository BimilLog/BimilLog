package jaeik.growfarm.controller.user;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 통합 컨트롤러</h2>
 * <p>
 * 외부 API 통합 및 기타 통합 기능을 담당하는 컨트롤러
 * SRP: 외부 시스템과의 통합 기능만 담당
 * </p>
 * <p>
 * 담당 기능:
 * - 건의사항 접수
 * - 카카오 친구 목록 조회 (외부 API 연동)
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserIntegrationController {

    private final UserService userService;

    /**
     * <h3>건의하기 API</h3>
     *
     * <p>
     * 사용자의 건의사항을 접수한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param reportDTO   건의 내용 DTO
     * @return 건의 접수 성공 메시지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
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
     * @param userDetails 현재 로그인한 사용자 정보
     * @param offset      페이지 오프셋
     * @return 카카오 친구 목록
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @PostMapping("/friendlist")
    public ResponseEntity<KakaoFriendListDTO> getFriendList(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             @RequestParam int offset) {
        return ResponseEntity.ok(userService.getFriendList(userDetails, offset));
    }
}