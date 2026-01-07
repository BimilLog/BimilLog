package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.event.FriendshipCreatedEvent;
import jaeik.bimillog.domain.friend.event.FriendshipDeletedEvent;
import jaeik.bimillog.domain.friend.repository.FriendRequestRepository;
import jaeik.bimillog.domain.friend.adapter.FriendToMemberAdapter;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
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
public class FriendshipCommandService {
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FriendToMemberAdapter friendToMemberAdapter;

    /**
     * 친구 생성
     */
    @Transactional
    public void createFriendship(Long memberId, Long friendId, Long friendRequestId) {

        // 친구가 실존하는지 확인
        Member friend = friendToMemberAdapter.findById(friendId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // 요청 받는 사람과 블랙리스트 관계인지 확인
        friendToMemberAdapter.checkMemberBlacklist(memberId, friendId);

        // 이미 친구가 되어잇는지 확인 (1,10)이 있으면 (10,1)도 있으면 안된다.
        checkFriendship(memberId, friendId);

        Member member = friendToMemberAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        Friendship friendship = Friendship.createFriendship(member, friend);
        friendshipRepository.save(friendship);

        // 요청 삭제
        friendRequestRepository.deleteById(friendRequestId);

        // Redis 친구 관계 추가 이벤트 발행
        eventPublisher.publishEvent(new FriendshipCreatedEvent(memberId, friendId));
    }

    /**
     * 친구 삭제
     */
    @Transactional
    public void deleteFriendship(Long memberId, Long friendshipId) {
        // 친구가 자신의 친구인지 확인한다.
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_SHIP_NOT_FOUND));

        boolean isParticipant = Objects.equals(friendship.getMember().getId(), memberId) ||
                Objects.equals(friendship.getFriend().getId(), memberId);
        if (!isParticipant) {
            throw new CustomException(ErrorCode.FRIEND_SHIP_DELETE_FORBIDDEN);
        }

        // Redis 친구 관계 삭제를 위해 memberId와 friendId 추출
        Long member1Id = friendship.getMember().getId();
        Long member2Id = friendship.getFriend().getId();

        friendshipRepository.delete(friendship);

        // Redis 친구 관계 삭제 이벤트 발행
        eventPublisher.publishEvent(new FriendshipDeletedEvent(member1Id, member2Id));
    }

    private void checkFriendship(Long memberId, Long friendId) {
        // 이미 친구가 되어있다.
        boolean aSendB = friendshipRepository.existsByMemberIdAndFriendId(memberId, friendId);

        if (aSendB) {
            throw new CustomException(ErrorCode.FRIEND_SHIP_ALREADY_EXIST);
        }

        // 이미 상대의 친구가 되어있다.
        boolean bSendA = friendshipRepository.existsByMemberIdAndFriendId(friendId, memberId);

        if (bSendA) {
            throw new CustomException(ErrorCode.FRIEND_SHIP_ALREADY_EXIST);
        }
    }
}
