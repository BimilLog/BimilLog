package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.jpa.Friendship;
import jaeik.bimillog.domain.friend.repository.FriendshipRepository;
import jaeik.bimillog.domain.global.out.GlobalMemberBlacklistAdapter;
import jaeik.bimillog.domain.global.out.GlobalMemberQueryAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendshipCommand {
    private final GlobalMemberBlacklistAdapter globalMemberBlacklistAdapter;
    private final GlobalMemberQueryAdapter globalMemberQueryAdapter;
    private final FriendshipRepository friendshipRepository;

    /**
     * 친구 생성
     */
    @Transactional
    public void createFriendship(Long memberId, Long friendId) {

        // 친구가 실존하는지 확인
        Member friend = globalMemberQueryAdapter.findById(friendId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        // 요청 받는 사람과 블랙리스트 관계인지 확인
        globalMemberBlacklistAdapter.checkMemberBlacklist(memberId, friendId);

        // 이미 친구가 되어잇는지 확인 (1,10)이 있으면 (10,1)도 있으면 안된다.
        checkFriendship(memberId, friendId);

        Member member = globalMemberQueryAdapter.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_USER_NOT_FOUND));

        Friendship friendship = Friendship.createFriendship(member, friend);
        friendshipRepository.save(friendship);
    }

    /**
     * 친구 삭제
     */
    @Transactional
    public void deleteFriendship(Long memberId, Long friendshipId) {
        // 친구가 자신의 친구인지 확인한다.
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_SHIP_NOT_FOUND));

        if (!Objects.equals(friendship.getMember().getId(), memberId)) {
            if (!Objects.equals(friendship.getFriend().getId(), memberId)) {
                throw new CustomException(ErrorCode.FRIEND_SHIP_DELETE_FORBIDDEN);
            }
        }

        friendshipRepository.delete(friendship);
    }

    private void checkFriendship(Long memberId, Long friendId) {
        // 이미 친구가 되어있다.
        boolean aSendB = friendshipRepository.existsBySenderIdAndReceiverId(memberId, friendId);

        if (aSendB) {
            throw new CustomException(ErrorCode.FRIEND_SHIP_ALREADY_EXIST);
        }

        // 이미 상대의 친구가 되어있다.
        boolean bSendA = friendshipRepository.existsBySenderIdAndReceiverId(friendId, memberId);

        if (bSendA) {
            throw new CustomException(ErrorCode.FRIEND_SHIP_ALREADY_EXIST);
        }
    }
}
