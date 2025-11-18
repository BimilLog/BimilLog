package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.print.Pageable;

@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/friend")
public class FriendshipQueryController {

    /**
     * 친구 조회
     */
    @GetMapping("/myfriend")
    public void getMyFriend(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Pageable pageable) {


    }
}
