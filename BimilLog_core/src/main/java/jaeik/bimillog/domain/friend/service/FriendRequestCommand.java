package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendRequest;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendRequestCommand {
    private final FriendRequestRepository friendRequestRepository;

    /**
     * 보낸 요청 취소
     * @param memberId
     * @param friendRequestId
     */
    @Transactional
    public void cancelFriendRequest(Long memberId, Long friendRequestId) {
        // 요청ID가 실제하는지 확인 && 요청ID가 요청자의 ID 소속이 맞는지 확인
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!Objects.equals(friendRequest.getSender().getId(), memberId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_CANCEL_FORBIDDEN);
        }

        // 요청 삭제
        friendRequestRepository.deleteById(friendRequestId);
    }

    /**
     * 받은 요청 거절
     */
    @Transactional
    public void rejectFriendRequest(Long memberId, Long friendRequestId) {
        // 요청ID가 실제하는지 확인 && 요청ID가 수신자의ID 소속이 맞는지 확인
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!Objects.equals(friendRequest.getReceiver().getId(), memberId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_REJECT_FORBIDDEN);
        }

        // 요청 삭제
        friendRequestRepository.deleteById(friendRequestId);
    }
}
