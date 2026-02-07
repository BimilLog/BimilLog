package jaeik.bimillog.domain.post.adapter;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>PostToMemberAdapter</h2>
 * <p>Post 도메인에서 Member 도메인으로의 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.4.0
 */
@Component
@RequiredArgsConstructor
public class PostToMemberAdapter {
    private final MemberQueryService memberQueryService;
    private final MemberBlacklistService memberBlacklistService;

    /**
     * <h3>자신의 블랙리스트한 사람의 ID와 나를 블랙리스트로 한 사람의 ID 조회</h3>
     */
    public List<Long> getInterActionBlacklist(Long memberId) {
        return memberBlacklistService.getInterActionBlacklist(memberId);
    }

    /**
     * <h3>사용자 ID로 멤버조회</h3>
     * *
     * @param memberId 참조할 사용자 ID
     * @return Member 멤버 데이터
     */
    public Member getMember(Long memberId) {
        return memberQueryService.findById(memberId);
    }
}
