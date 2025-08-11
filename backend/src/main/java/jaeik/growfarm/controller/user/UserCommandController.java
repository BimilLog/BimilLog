package jaeik.growfarm.controller.user;

import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 수정 컨트롤러</h2>
 * <p>
 * 사용자 관련 수정/변경 작업만 담당하는 컨트롤러
 * SRP: 사용자 데이터 수정 기능만 담당 (Command in CQRS)
 * </p>
 * <p>
 * 담당 기능:
 * - 닉네임 변경
 * - 설정 수정
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserCommandController {

    private final UserService userService;

    /**
     * <h3>닉네임 변경 API</h3>
     *
     * <p>
     * 닉네임을 변경한다.
     * </p>
     *
     * @param userNameDTO 닉네임 변경 요청 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 변경 성공 메시지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @RequestBody @Valid UserNameDTO userNameDTO) {
        // 보안 강화: userId가 제공된 경우 현재 사용자와 일치하는지 검증
        if (userNameDTO.getUserId() != null && 
            !userNameDTO.getUserId().equals(userDetails.getUserId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        
        userService.updateUserName(userNameDTO.getUserName(), userDetails);
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
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
     * @version 2.0.0
     * @since 2.0.0
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody SettingDTO settingDTO,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateSetting(settingDTO, userDetails);
        return ResponseEntity.ok("설정 수정 완료");
    }
}