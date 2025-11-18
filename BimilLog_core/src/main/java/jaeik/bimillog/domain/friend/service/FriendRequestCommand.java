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

    // 보낸 요청 취소
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
}
