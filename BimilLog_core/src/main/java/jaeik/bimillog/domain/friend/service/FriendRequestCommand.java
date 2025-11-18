package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.FriendRequest;
import jaeik.bimillog.domain.friend.event.FriendEvent;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
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
public class FriendRequestCommand {
    private final FriendRequestRepository friendRequestRepository;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;
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

    /**
     * 친구 요청 전송
     */
    public void sendFriendRequest(Long memberId, Long receiveMemberId) {
        // 요청 받는 사람이 실존하는지 확인
        Member receiver = globalMemberQueryAdapter.findById(receiveMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // 요청 받는 사람과 블랙리스트 관계인지 확인
        globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, receiveMemberId);

        Member sender = globalMemberQueryAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        FriendRequest friendRequest = FriendRequest.createFriendRequest(sender, receiver);

        // 이미 요청이 존재 하는지 확인하지 않고 바로 저장 오류시 존재하는 것
        friendRequestRepository.save(friendRequest);

        // 비동기로 SSE와 FCM 발송, 알림DB 저장
        eventPublisher.publishEvent(new FriendEvent(
                receiveMemberId,
                receiver.getMemberName() + "님 에게서 친구 요청이 도착했습니다.",
                receiver.getMemberName() + "님 에게서 친구 요청이 도착했습니다.",
                "비밀로그에서 확인해보세요!"
        ));
    }
}
