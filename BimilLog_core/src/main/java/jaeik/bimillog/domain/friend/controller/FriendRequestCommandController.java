package jaeik.bimillog.domain.friend.controller;

import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.service.FriendRequestCommand;
import jaeik.bimillog.domain.friend.service.FriendRequestQuery;
import jaeik.bimillog.domain.friend.service.FriendshipCommand;
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
    private final FriendshipCommand friendshipCommand;

    /**
     * 보낸 친구 요청 취소 API<br>
     * 친구 요청을 삭제하고 보낸 친구 요청 조회 데이터 반환
     */
    @DeleteMapping("/send/{friendRequestId}")
    public ResponseEntity<Page<FriendSenderRequest>> cancelMyFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                           @PathVariable Long friendRequestId,
                                                                           Pageable pageable) {

        friendRequestCommand.cancelFriendRequest(userDetails.getMemberId(), friendRequestId);
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQuery.getFriendSendRequest(userDetails.getMemberId(), pageable);
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

        friendRequestCommand.deleteFriendRequest(userDetails.getMemberId(), friendRequestId);
        Page<FriendReceiverRequest> friendSenderRequests = friendRequestQuery.getFriendReceiveRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }

    /**
     * 받은 친구 요청 승인 API<br>
     * 친구의 Id를 조회, 친구 관계 설정, 친구 요청 삭제
     */
    @PostMapping("/receive/{friendRequestId}")
    public ResponseEntity<String> acceptReceiveFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                  @PathVariable Long friendRequestId,
                                                                                  Pageable pageable) {

        Long senderId = friendRequestQuery.getSenderId(userDetails.getMemberId(), friendRequestId);
        friendshipCommand.createFriendship(userDetails.getMemberId(), senderId);
        friendRequestCommand.deleteFriendRequest(userDetails.getMemberId(), friendRequestId);
        return ResponseEntity.ok("친구가 등록되었습니다.");
    }
    /**
     * 친구 요청 전송 API<br>
     * 친구 요청을 만들고 보낸 친구 요청 조회 데이터 반환
     */
    @PostMapping("/send")
    public ResponseEntity<Page<FriendSenderRequest>> sendFriendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                       @RequestBody FriendSenderRequest friendSenderRequest,
                                                                       Pageable pageable) {

        friendRequestCommand.sendFriendRequest(userDetails.getMemberId(), friendSenderRequest.getReceiverMemberId());
        Page<FriendSenderRequest> friendSenderRequests = friendRequestQuery.getFriendSendRequest(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(friendSenderRequests);
    }
}
