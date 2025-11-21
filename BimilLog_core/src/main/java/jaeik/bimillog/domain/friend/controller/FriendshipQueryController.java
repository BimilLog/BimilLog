package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.service.FriendshipQuery;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/friend")
public class FriendshipQueryController {
    private final FriendshipQuery friendshipQuery;

    /**
     * 친구 조회
     */
    @GetMapping("/list")
    public ResponseEntity<Page<Friend>> getMyFriend(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      Pageable pageable) {

        Page<Friend> myFriendPages = friendshipQuery.getMyFriendList(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(myFriendPages);
    }
}
