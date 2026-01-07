package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
import jaeik.bimillog.domain.global.event.CheckBlacklistEvent;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendRequestCommandService {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendToMemberAdapter friendToMemberAdapter;
    private final ApplicationEventPublisher eventPublisher;

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
     * 받은 요청 삭제
     */
    @Transactional
    public void deleteFriendRequest(Long memberId, Long friendRequestId) {
        // 친구 요청이 실제하는지 확인
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 친구 요청이 수신자에게 온 것이 맞는지 확인
        if (!Objects.equals(friendRequest.getReceiver().getId(), memberId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_REJECT_FORBIDDEN);
        }

        // 요청 삭제
        friendRequestRepository.deleteById(friendRequestId);
    }

    /**
     * 친구 요청 전송
     */
    @Transactional
    public void sendFriendRequest(Long memberId, Long receiveMemberId) {
        // 자기 자신에게 요청 금지
        if (Objects.equals(memberId, receiveMemberId)) {
            throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST_FORBIDDEN);
        }

        // 요청 받는 사람이 실존하는지 확인
        Member receiver = friendToMemberAdapter.findById(receiveMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // 요청 받는 사람과 블랙리스트 관계인지 확인
        eventPublisher.publishEvent(new CheckBlacklistEvent(memberId, receiveMemberId));

        // 이미 요청이 존재 하는지 확인 (1,10)이 있으면 (10,1)도 있으면 안된다.
        checkFriendRequest(memberId, receiveMemberId);

        Member sender = friendToMemberAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        FriendRequest friendRequest = FriendRequest.createFriendRequest(sender, receiver);

        friendRequestRepository.save(friendRequest);

        // 비동기로 SSE와 FCM 발송, 알림DB 저장
        eventPublisher.publishEvent(new FriendEvent(
                receiveMemberId,
                sender.getMemberName() + "님 에게서 친구 요청이 도착했습니다.",
                sender.getMemberName()
        ));
    }

    private void checkFriendRequest(Long memberId, Long receiveMemberId) {
        // 이미 요청이 존재한다.
        boolean aSendB = friendRequestRepository.existsBySenderIdAndReceiverId(memberId, receiveMemberId);

        if (aSendB) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_SEND);
        }

        // 이미 상대가 요청을 보냈다.
        boolean bSendA = friendRequestRepository.existsBySenderIdAndReceiverId(receiveMemberId, memberId);

        if (bSendA) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_RECEIVE);
        }
    }
}
