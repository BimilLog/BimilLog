package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.member.out.MemberRepository;
import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>사용자 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 사용자 조회 기능을 구현하는 어댑터입니다.</p>
 * <p>GlobalUserQueryPort를 구현하여 도메인 간 사용자 조회 기능을 통합 제공합니다.</p>
 * <p>Member 도메인의 UserQueryUseCase를 통해 실제 사용자 데이터에 접근합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalMemberQueryAdapter {

    private final MemberQueryService memberQueryService;
    private final MemberRepository memberRepository;

    /**
     * <h3>사용자 ID로 사용자 조회</h3>
     * <p>특정 ID에 해당하는 사용자 엔티티를 조회합니다.</p>
     * <p>Member 도메인의 UserQueryUseCase를 통해 사용자 정보를 조회합니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    public Optional<Member> findById(Long memberId) {
        return memberQueryService.findById(memberId);
    }

    /**
     * <h3>사용자명으로 사용자 조회</h3>
     * <p>특정 사용자명에 해당하는 사용자 엔티티를 조회합니다.</p>
\     *
     * @param memberName 조회할 사용자명
     * @return Optional&lt;Member&gt; 조회된 사용자 객체 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    public Optional<Member> findByMemberName(String memberName) {
        return memberQueryService.findByMemberName(memberName);
    }

    /**
     * <h3>사용자 ID로 JPA 프록시 참조 조회</h3>
     * <p>실제 데이터베이스 조회 없이 사용자 ID를 가진 Member 프록시 객체를 반환합니다.</p>
     * <p>Member 도메인의 UserQueryUseCase를 통해 프록시 객체를 생성합니다.</p>
     *
     * @param memberId 참조할 사용자 ID
     * @return Member 프록시 객체 (실제 데이터는 지연 로딩)
     * @author Jaeik
     * @since 2.0.0
     */
    public Member getReferenceById(Long memberId) {
        return memberQueryService.getReferenceById(memberId);
    }

    /**
     * <h3>최근 가입한 사용자 ID 목록 조회</h3>
     * <p>최근 가입한 순서로 사용자 ID 목록을 조회합니다.</p>
     * <p>특정 사용자 ID를 제외하고 조회할 수 있습니다.</p>
     *
     * @param excludeIds 제외할 사용자 ID 목록
     * @param limit 조회할 최대 개수
     * @return List&lt;Long&gt; 최근 가입한 사용자 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    public List<Long> findRecentMemberIds(Set<Long> excludeIds, int limit) {
        // 제외할 ID를 고려하여 더 많은 데이터를 조회 (최대 limit * 2)
        int fetchSize = Math.min(limit * 2, 100);

        PageRequest pageRequest = PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Member> recentMembers = memberRepository.findAll(pageRequest).getContent();

        return recentMembers.stream()
                .map(Member::getId)
                .filter(id -> !excludeIds.contains(id))
                .limit(limit)
                .collect(Collectors.toList());
    }
}