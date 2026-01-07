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
     * <h3>사용자 ID로 JPA 프록시 참조 조회</h3>
     * <p>실제 데이터베이스 조회 없이 사용자 ID를 가진 Member 프록시 객체를 반환합니다.</p>
     *
     * @param memberId 참조할 사용자 ID
     * @return Member 프록시 객체 (실제 데이터는 지연 로딩)
     */
    public Member getReferenceById(Long memberId) {
        return memberQueryService.getReferenceById(memberId);
    }
}
