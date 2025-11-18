package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.service.FriendRequestCommand;
import jaeik.bimillog.domain.friend.service.FriendRequestQuery;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/friend")
public class FriendRequestCommandController {
    private final FriendRequestQuery friendRequestQuery;
    private final FriendRequestCommand friendRequestCommand;

    /**
     * 보낸 친구 요청 취소 API
     */
    @DeleteMapping("/send/{friendRequestId}")
    public ResponseEntity<Page<FriendSenderRequest>> cancelMyFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                           @PathVariable Long friendRequestId,
                                                                           Pageable pageable) {

        friendRequestCommand.cancelFriendRequest(userDetails.getMemberId(), friendRequestId);
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQuery.getFriendSendRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }

}
