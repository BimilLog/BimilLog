package jaeik.growfarm.domain.user.infrastructure.adapter.in.web;

import jaeik.growfarm.domain.user.application.port.in.UserCommandUseCase;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.dto.user.UserNameDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserCommandController {

    private final UserCommandUseCase userCommandUseCase;

    @PostMapping("/username")
    public ResponseEntity<String> updateUserName(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @RequestBody @Valid UserNameDTO userNameDTO) {
        userCommandUseCase.updateUserName(userDetails.getUserId(), userNameDTO.getUserName());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    @PostMapping("/setting")
    public ResponseEntity<String> updateSetting(@RequestBody SettingDTO settingDTO,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        userCommandUseCase.updateUserSettings(userDetails.getUserId(), settingDTO);
        return ResponseEntity.ok("설정 수정 완료");
    }

}