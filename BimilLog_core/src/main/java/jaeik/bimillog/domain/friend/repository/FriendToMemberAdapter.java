package jaeik.bimillog.domain.friend.repository;

import jaeik.bimillog.domain.friend.entity.Friend;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.domain.member.service.MemberFriendService;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class FriendToMemberAdapter {
    private final MemberFriendService memberFriendService;
    private final MemberQueryService memberQueryService;
    private final MemberBlacklistService memberBlacklistService;

    // 친구 추가 정보 조회
    public List<Friend.FriendInfo> addMyFriendInfo(List<Long> friendIds) {
        return memberFriendService.addMyFriendInfo(friendIds);
    }

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     */
    public Optional<Member> findById(Long memberId) {
        return memberQueryService.findById(memberId);
    }

    public void checkMemberBlacklist(Long memberId, Long targetMemberId) {
        boolean isBlacklisted = memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);
        if (isBlacklisted) {
            throw new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);
        }
    }
}
