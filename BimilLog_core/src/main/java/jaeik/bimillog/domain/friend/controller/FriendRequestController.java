package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.service.FriendRequestQuery;
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
public class FriendRequestController {
    private final FriendRequestQuery friendRequestQuery;

    /**
     * 보낸 친구 요청 조회 API
     */
    @GetMapping("/send")
    public ResponseEntity<Page<FriendSenderRequest>> getFriendSendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                         Pageable pageable) {
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQuery.getFriendSendRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }

    /**
     * 받은 친구 요청 조회 API
     */
    @GetMapping("/receive")
    public ResponseEntity<Page<FriendReceiverRequest>> getFriendReceiveRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                            Pageable pageable) {
        Page<FriendReceiverRequest> friendSenderRequests = friendRequestQuery.getFriendReceiveRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }
}
