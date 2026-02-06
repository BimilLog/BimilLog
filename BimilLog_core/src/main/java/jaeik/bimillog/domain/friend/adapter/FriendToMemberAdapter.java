package jaeik.bimillog.domain.friend.adapter;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendToMemberAdapter {
    private final MemberQueryService memberQueryService;

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     */
    public Member findById(Long memberId) {
        return memberQueryService.findById(memberId);
    }
}
