package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 글, 댓글 상호작용시 블랙리스트에 서로가 있는지 조회하는 용도
 */
@Component
@RequiredArgsConstructor
public class GlobalMemberBlacklistAdapter {
    private MemberBlacklistService memberBlacklistService;

    public void checkMemberBlacklist(Long memberId, Long targetMemberId) {
        boolean BlacklistCheck = memberBlacklistService.checkMemberBlacklist(memberId, targetMemberId);
        if (BlacklistCheck) {
            throw new CustomException(ErrorCode.BLACKLIST_MEMBER_PAPER_FORBIDDEN);
        }
    }
}
