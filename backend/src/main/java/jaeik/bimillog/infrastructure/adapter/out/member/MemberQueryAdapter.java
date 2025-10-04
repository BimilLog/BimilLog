package jaeik.bimillog.infrastructure.adapter.out.member;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.application.service.MemberQueryService;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.member.QMember;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>사용자 정보 조회를 위한 영속성 어댑터</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 카카오 친구 이름 매핑</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class MemberQueryAdapter implements MemberQueryPort {

    private final MemberRepository userRepository;
    private final SettingRepository settingRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final QMember member = QMember.member;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회합니다.</p>
     * <p>{@link MemberQueryService}에서 기본 사용자 조회 시 호출됩니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * <h3>ID와 설정을 포함한 사용자 조회</h3>
     * <p>주어진 ID로 사용자 정보를 조회하며, 연관된 설정 정보도 함께 가져옵니다.</p>
     * <p>{@link MemberQueryService}에서 설정 정보가 필요한 조회 시 호출됩니다.</p>
     *
     * @param id 사용자 ID
     * @return Optional<Member> 조회된 사용자 객체 (설정 정보 포함). 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByIdWithSetting(Long id) {
        Member result = jpaQueryFactory
                .selectFrom(member)
                .leftJoin(member.setting).fetchJoin()
                .where(member.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 조회</h3>
     * <p>주어진 소셜 제공자와 소셜 ID로 사용자 정보를 조회합니다.</p>
     * <p>{@link MemberQueryService}에서 소셜 로그인 사용자 조회 시 호출됩니다.</p>
     *
     * @param provider 소셜 제공자
     * @param socialId 소셜 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return userRepository.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>닉네임 존재 여부 확인</h3>
     * <p>주어진 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link MemberQueryService}에서 닉네임 중복 확인 시 호출됩니다.</p>
     *
     * @param memberName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberName(String memberName) {
        return userRepository.existsByMemberName(memberName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>주어진 닉네임으로 사용자 정보를 조회합니다.</p>
     * <p>{@link MemberQueryService}에서 닉네임 기반 사용자 조회 시 호출됩니다.</p>
     *
     * @param memberName 조회할 닉네임
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByMemberName(String memberName) {
        return userRepository.findByMemberName(memberName);
    }

    /**
     * <h3>ID로 설정 조회</h3>
     * <p>주어진 ID로 설정 정보를 조회합니다.</p>
     * <p>{@link MemberQueryService}에서 JWT 토큰 기반 설정 조회 시 호출됩니다.</p>
     *
     * @param settingId 조회할 설정 ID
     * @return Optional<Setting> 조회된 설정 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Setting> findSettingById(Long settingId) {
        return settingRepository.findById(settingId);
    }

    /**
     * <h3>주어진 순서대로 사용자 이름 조회</h3>
     * <p>주어진 소셜 ID 목록에 해당하는 사용자 이름들을 요청된 순서대로 조회합니다.</p>
     * <p>카카오 친구 목록 매핑에 사용됩니다.</p>
     * <p>{@link MemberQueryService}에서 카카오 친구 목록 매핑 시 호출됩니다.</p>
     *
     * @param socialIds 조회할 소셜 ID 문자열 리스트
     * @return List<String> 조회된 사용자 이름 리스트
     * @author jaeik
     * @since  2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> findMemberNamesInOrder(List<String> socialIds) {
        if (socialIds == null || socialIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> results = jpaQueryFactory
                .select(member.socialId, member.memberName)
                .from(member)
                .where(member.socialId.in(socialIds))
                .fetch();

        Map<String, String> socialIdToUserName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(member.socialId),
                        tuple -> Optional.ofNullable(tuple.get(member.memberName)).orElse(""),
                        (existing, replacement) -> existing // Handle duplicate keys if any
                ));

        return socialIds.stream()
                .map(id -> socialIdToUserName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }

    /**
     * <h3>ID로 사용자 참조 가져오기</h3>
     * <p>주어진 ID의 사용자 엔티티 참조를 가져옵니다.</p>
     * <p>JPA 연관 관계 설정 시 성능 최적화를 위해 사용됩니다.</p>
     * <p>{@link MemberQueryService}에서 사용자 엔티티 참조 생성 시 호출됩니다.</p>
     *
     * @param memberId 참조를 가져올 사용자 ID
     * @return Member 사용자 엔티티 참조
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Member getReferenceById(Long memberId) {
        return userRepository.getReferenceById(memberId);
    }
}
