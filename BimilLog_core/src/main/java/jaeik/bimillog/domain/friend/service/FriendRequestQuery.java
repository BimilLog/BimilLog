package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.repository.FriendRequestQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendRequestQuery {
    private final FriendRequestQueryRepository friendRequestQueryRepository;

    // 보낸 친구 요청 조회
    public Page<FriendSenderRequest> getFriendSendRequest(Long memberId, Pageable pageable) {
        return friendRequestQueryRepository.findAllBySenderId(memberId, pageable);
    }

    // 받은 친구 요청 조회
    public Page<FriendReceiverRequest> getFriendReceiveRequest(Long memberId, Pageable pageable) {
        return friendRequestQueryRepository.findAllByReceiveId(memberId, pageable);
    }
}
