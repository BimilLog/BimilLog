package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendReceiverRequest;
import jaeik.bimillog.domain.friend.entity.FriendSenderRequest;
import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.repository.FriendRequestQueryRepository;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendRequestQueryService {
    private final FriendRequestQueryRepository friendRequestQueryRepository;
    private final FriendRequestRepository friendRequestRepository;

    // 보낸 친구 요청 조회
    @Transactional(readOnly = true)
    public Page<FriendSenderRequest> getFriendSendRequest(Long memberId, Pageable pageable) {
        return friendRequestQueryRepository.findAllBySenderId(memberId, pageable);
    }

    // 받은 친구 요청 조회
    @Transactional(readOnly = true)
    public Page<FriendReceiverRequest> getFriendReceiveRequest(Long memberId, Pageable pageable) {
        return friendRequestQueryRepository.findAllByReceiveId(memberId, pageable);
    }

    // 특정 친구 요청의 보낸 사람의 ID 조회
    @Transactional(readOnly = true)
    public Long getSenderId(Long receiverId, Long friendRequestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 친구 요청이 수신자에게 온 것이 맞는지 확인
        if (!Objects.equals(friendRequest.getReceiver().getId(), receiverId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_REJECT_FORBIDDEN);
        }

        return friendRequest.getSender().getId();
    }
}
