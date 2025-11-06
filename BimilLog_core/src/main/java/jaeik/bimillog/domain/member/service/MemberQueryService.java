
package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.member.application.port.in.MemberQueryUseCase;
import jaeik.bimillog.domain.member.application.port.out.MemberQueryPort;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.domain.member.in.web.MemberQueryController;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>사용자 조회 서비스</h2>
 * <p>UserQueryUseCase의 구현체로 사용자 정보 조회 로직을 담당합니다.</p>
 * <p>사용자 엔티티 조회, 설정 조회, 닉네임 검증</p>
 * <p>소셜 로그인 사용자 조회, 토큰 기반 인증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberQueryUseCase {

    private final MemberQueryPort memberQueryPort;

    /**
     * <h3>ID로 사용자 조회</h3>
     * <p>사용자 ID를 사용하여 사용자를 조회합니다.</p>
     * <p>{@link MemberQueryUseCase}에서 기본 사용자 조회 시 호출됩니다.</p>
     *
     * @param id 사용자의 고유 ID
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findById(Long id) {
        return memberQueryPort.findById(id);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     * <p>해당 닉네임을 가진 사용자가 존재하는지 확인합니다.</p>
     * <p>{@link MemberQueryController}에서 닉네임 중복 확인 API 시 호출됩니다.</p>
     *
     * @param memberName 확인할 닉네임
     * @return boolean 존재하면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByMemberName(String memberName) {
        return memberQueryPort.existsByMemberName(memberName);
    }

    /**
     * <h3>닉네임으로 사용자 조회</h3>
     * <p>닉네임을 사용하여 사용자를 조회합니다.</p>
     * <p>{@link MemberQueryUseCase}에서 닉네임 기반 사용자 조회 시 호출됩니다.</p>
     *
     * @param memberName 사용자 닉네임
     * @return Optional<Member> 조회된 사용자 객체. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByMemberName(String memberName) {
        return memberQueryPort.findByMemberName(memberName);
    }


    /**
     * <h3>ID로 사용자 프록시 조회</h3>
     * <p>실제 쿼리 없이 ID를 가진 사용자의 프록시(참조) 객체를 반환합니다.</p>
     * <p>JPA 연관 관계 설정 시 사용됩니다.</p>
     * <p>{@link MemberQueryUseCase}에서 사용자 엔티티 참조 생성 시 호출됩니다.</p>
     *
     * @param memberId 사용자 ID
     * @return Member 프록시 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Member getReferenceById(Long memberId) {
        return memberQueryPort.getReferenceById(memberId);
    }

    /**
     * <h3>설정 ID로 설정 조회</h3>
     * <p>JWT 토큰의 settingId를 활용하여 설정 정보를 조회합니다.</p>
     * <p>Member 엔티티 전체 조회 없이 Setting만 직접 조회합니다.</p>
     * <p>{@link MemberQueryController}에서 사용자 설정 조회 API 시 호출됩니다.</p>
     *
     * @param settingId 설정 ID
     * @return 설정 엔티티
     * @throws MemberCustomException 설정을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional(readOnly = true)
    public Setting findBySettingId(Long settingId) {
        return memberQueryPort.findSettingById(settingId)
                .orElseThrow(() -> new MemberCustomException(MemberErrorCode.SETTINGS_NOT_FOUND));
    }

    /**
     * <h3>소셜 제공자와 소셜 ID로 사용자 조회</h3>
     * <p>특정 소셜 플랫폼의 소셜 ID에 해당하는 사용자를 조회합니다.</p>
     * <p>소셜 로그인 단계에서 기존 회원 여부를 확인할 때 사용됩니다.</p>
     *
     * @param provider 소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialId 소셜 플랫폼에서 제공하는 고유 ID
     * @return Optional&lt;Member&gt; 조회된 사용자 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Member> findByProviderAndSocialId(SocialProvider provider, String socialId) {
        return memberQueryPort.findByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>여러 사용자 ID로 사용자명 배치 조회</h3>
     * <p>여러 사용자 ID에 해당하는 사용자명을 한 번에 조회합니다.</p>
     *
     * @param memberIds 조회할 사용자 ID 목록
     * @return Map<Long, String> 사용자 ID를 키로, 사용자명을 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findMemberNamesByIds(List<Long> memberIds) {
        return memberQueryPort.findMemberNamesByIds(memberIds);
    }

    /**
     * <h3>모든 회원 페이지 조회</h3>
     * <p>페이지 정보에 따라 전체 회원 목록을 조회합니다.</p>
     *
     * @param pageable 페이지 정보
     * @return Page<Member> 조회된 회원 페이지
     * @since 2.1.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Member> findAllMembers(Pageable pageable) {
        return memberQueryPort.findAllMembers(pageable);
    }

    /**
     * <h3>사용자명 검색</h3>
     * <p>검색어로 사용자명을 검색합니다.</p>
     * <p>검색 전략: 4글자 이상이면 접두사 검색, 그 외에는 부분 검색을 사용합니다.</p>
     * <p>{@link MemberQueryController}에서 사용자 검색 API 시 호출됩니다.</p>
     *
     * @param query    검색어
     * @param pageable 페이징 정보
     * @return Page<String> 검색된 사용자명 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<String> searchMembers(String query, Pageable pageable) {
        // 전략: 4글자 이상 → 접두사 검색 (인덱스 활용)
        if (query.length() >= 4) {
            return memberQueryPort.findByPrefixMatch(query, pageable);
        }

        // 그 외 → 부분 검색
        return memberQueryPort.findByPartialMatch(query, pageable);
    }
}
