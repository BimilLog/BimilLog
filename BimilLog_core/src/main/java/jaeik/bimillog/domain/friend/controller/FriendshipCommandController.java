package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.service.FriendshipCommand;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/friend")
public class FriendshipCommandController {
    private final FriendshipCommand friendshipCommand;

    /**
     * 친구 삭제 API
     */
    @DeleteMapping("/friendship/{friendshipId}")
    public ResponseEntity<String> deleteFriendship(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @PathVariable Long friendshipId) {
        friendshipCommand.deleteFriendship(userDetails.getMemberId(), friendshipId);
        return ResponseEntity.ok("친구가 삭제되었습니다.");
    }
}
