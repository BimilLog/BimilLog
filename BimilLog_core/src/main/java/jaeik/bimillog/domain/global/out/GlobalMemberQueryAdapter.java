package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>사용자 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 사용자 조회 기능을 구현하는 어댑터입니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalMemberQueryAdapter {
    private final MemberQueryService memberQueryService;

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