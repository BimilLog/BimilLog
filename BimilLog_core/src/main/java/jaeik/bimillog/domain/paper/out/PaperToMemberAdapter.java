package jaeik.bimillog.domain.paper.out;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>PaperToMemberAdapter</h2>
 * <p>
 * Paper 도메인에서 Member 도메인으로의 어댑터입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@RequiredArgsConstructor
public class PaperToMemberAdapter {
    private final MemberQueryService memberQueryService;
    private final MemberBlacklistService memberBlacklistService;


    /**
     * <h3>여러 사용자 ID로 사용자명 배치 조회</h3>
     * <p>여러 사용자 ID에 해당하는 사용자명을 한 번에 조회합니다.</p>
     *
     * @param memberIds 조회할 사용자 ID 목록
     * @return Map&lt;Long, String&gt; 사용자 ID를 키로, 사용자명을 값으로 하는 맵
     */
    public Map<Long, String> findMemberNamesByIds(List<Long> memberIds) {
        return memberQueryService.findMemberNamesByIds(memberIds);
    }

    /**
     * <h3>사용자명으로 사용자 조회</h3>
     * <p>특정 사용자명에 해당하는 사용자 엔티티를 조회합니다.</p>
     *
     * @param memberName 조회할 사용자명
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     */
    public Optional<Member> findByMemberName(String memberName) {
        return memberQueryService.findByMemberName(memberName);
    }

    /**
     * <h3>두 멤버가 블랙리스트 관계인지 체크</h3>
     */
    public void checkMemberBlacklist(Long memberId, Long targetMemberId) {
        boolean isBlacklisted = memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);
        if (isBlacklisted) {
            throw new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);
        }
    }

}
