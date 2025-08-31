package jaeik.bimillog.infrastructure.adapter.user.in.web;

import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.SettingDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.UserNameDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>사용자 명령 컨트롤러</h2>
 * <p>사용자 관련 명령 요청을 처리하는 컨트롤러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserCommandController {

    private final UserCommandUseCase userCommandUseCase;

    /**
     * <h3>닉네임 변경 API</h3>
     * <p>사용자의 닉네임을 변경하는 요청을 처리</p>
     *
     * @param userDetails 사용자 인증 정보
     * @param userNameDTO 닉네임 DTO
     * @return 닉네임 변경 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestBody @Valid UserNameDTO userNameDTO) {
        userCommandUseCase.updateUserName(userDetails.getUserId(), userNameDTO.getUserName());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    /**
     * <h3>설정 수정 API</h3>
     * <p>사용자의 설정을 수정하는 요청을 처리</p>
     *
     * @param settingDTO 설정 DTO
     * @param userDetails 사용자 인증 정보
     * @return 설정 수정 완료 메시지
     * @since 2.0.0
     * @author Jaeik
     */
    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody SettingDTO settingDTO,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        userCommandUseCase.updateUserSettings(userDetails.getUserId(), settingDTO.toSettingVO());
        return ResponseEntity.ok("설정 수정 완료");
    }

}