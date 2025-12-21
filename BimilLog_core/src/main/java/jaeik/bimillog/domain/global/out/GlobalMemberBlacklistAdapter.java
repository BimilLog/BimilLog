package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>여러도메인에서 멤버 블랙리스트를 조회하는 어댑터</h2>
 */
@Component
@RequiredArgsConstructor
public class GlobalMemberBlacklistAdapter {
    private final MemberBlacklistService memberBlacklistService;

    /**
     * <h3>두 멤버가 블랙리스트 관계인지 체크</h3>
     */
    public void checkMemberBlacklist(Long memberId, Long targetMemberId) {
        boolean isBlacklisted = memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);
        if (isBlacklisted) {
            throw new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);
        }
    }

    /**
     * <h3>자신의 블랙리스트한 사람의 ID와 나를 블랙리스트로 한 사람의 ID 조회</h3>
     */
    public List<Long> getInterActionBlacklist(Long memberId) {
        return memberBlacklistService.getInterActionBlacklist(memberId);
    }
}
