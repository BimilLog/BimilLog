package jaeik.growfarm.domain.user.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>사용자 조회 컨트롤러</h2>
 * <p>사용자 관련 조회 요청을 처리하는 컨트롤러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserQueryController {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>닉네임 중복 확인 API</h3>
     * <p>사용자의 닉네임이 이미 사용 중인지 확인하는 요청을 처리</p>
     *
     * @param userName 닉네임
     * @return 닉네임 사용 가능 여부
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String userName) {
        boolean isAvailable = !userQueryUseCase.existsByUserName(userName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>사용자 설정 조회 API</h3>
     * <p>사용자의 설정 정보를 조회하는 요청을 처리</p>
     *
     * @param userDetails 사용자 인증 정보
     * @return 사용자 설정 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SettingDTO settingDTO = userQueryUseCase.findById(userDetails.getUserId())
                .map(user -> new SettingDTO(user.getSetting()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(settingDTO);
    }
}