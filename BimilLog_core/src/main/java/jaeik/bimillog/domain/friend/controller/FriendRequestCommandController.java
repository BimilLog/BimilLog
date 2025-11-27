package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.service.FriendRequestCommandService;
import jaeik.bimillog.domain.friend.service.FriendRequestQueryService;
import jaeik.bimillog.domain.friend.service.FriendshipCommandService;
import jaeik.bimillog.domain.friend.service.FriendshipQueryService;
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
    private final FriendRequestQueryService friendRequestQueryService;
    private final FriendRequestCommandService friendRequestCommandService;
    private final FriendshipCommandService friendshipCommandService;
    private final FriendshipQueryService friendshipQueryService;

    /**
     * 보낸 친구 요청 취소 API<br>
     * 친구 요청을 삭제하고 보낸 친구 요청 조회 데이터 반환
     */
    @DeleteMapping("/send/{friendRequestId}")
    public ResponseEntity<Page<FriendSenderRequest>> cancelMyFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                           @PathVariable Long friendRequestId,
                                                                           Pageable pageable) {

        friendRequestCommandService.cancelFriendRequest(userDetails.getMemberId(), friendRequestId);
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQueryService.getFriendSendRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }

    /**
     * 받은 친구 요청 거절 API<br>
     * 친구 요청을 삭제하고 받은 친구 요청 조회 데이터 반환
     */
    @DeleteMapping("/receive/{friendRequestId}")
    public ResponseEntity<Page<FriendReceiverRequest>> rejectReceiveFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                           @PathVariable Long friendRequestId,
                                                                           Pageable pageable) {

        friendRequestCommandService.deleteFriendRequest(userDetails.getMemberId(), friendRequestId);
        Page<FriendReceiverRequest> friendSenderRequests = friendRequestQueryService.getFriendReceiveRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }

    /**
     * 받은 친구 요청 승인 API<br>
     * 친구의 Id를 조회, 친구 관계 설정 및 친구 요청 삭제, 친구 목록 반환
     */
    @PostMapping("/receive/{friendRequestId}")
    public ResponseEntity<Page<Friend>> acceptReceiveFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             @PathVariable Long friendRequestId,
                                                             Pageable pageable) {
        Long senderId = friendRequestQueryService.getSenderId(userDetails.getMemberId(), friendRequestId);
        friendshipCommandService.createFriendship(userDetails.getMemberId(), senderId, friendRequestId);
        Page<Friend> myFriendPages = friendshipQueryService.getMyFriendList(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(myFriendPages);
    }
    /**
     * 친구 요청 전송 API<br>
     * 친구 요청을 만들고 보낸 친구 요청 조회 데이터 반환
     */
    @PostMapping("/send")
    public ResponseEntity<Page<FriendSenderRequest>> sendFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                       @RequestBody FriendSenderRequest friendSenderRequest,
                                                                       Pageable pageable) {

        friendRequestCommandService.sendFriendRequest(userDetails.getMemberId(), friendSenderRequest.getReceiverMemberId());
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQueryService.getFriendSendRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }
}
