package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.service.FriendshipCommandService;
import jaeik.bimillog.domain.friend.service.FriendshipQueryService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final FriendshipCommandService friendshipCommandService;
    private final FriendshipQueryService friendshipQueryService;

    /**
     * 친구 삭제 API</br>
     * 친구 삭제 후 친구 목록 반환
     */
    @DeleteMapping("/friendship/{friendshipId}")
    public ResponseEntity<Page<Friend>> deleteFriendship(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @PathVariable Long friendshipId,
                                                   Pageable pageable) {
        friendshipCommandService.deleteFriendship(userDetails.getMemberId(), friendshipId);
        Page<Friend> myFriendPages = friendshipQueryService.getMyFriendList(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(myFriendPages);
    }
}
