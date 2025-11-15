package jaeik.bimillog.domain.global.entity;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.MemberRole;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <h2>커스텀 사용자 상세 정보 클래스</h2>
 * <p>
 * UserDetails 인터페이스를 구현하여 Spring Security에서 사용자 정보를 처리하는 클래스
 * </p>
 * <p>
 * JWT 토큰 생성/파싱과 Spring Security 인증에 모두 사용되는 통합 사용자 정보 객체
 * </p>
 *
 * @author Jaeik
 * @version 3.0.0
 * @since 2.0.0
 */
@Getter
@Builder
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String socialId;
    private final SocialProvider provider;
    private final Long settingId;
    private final String socialNickname;
    private final String thumbnailImage;
    private final String memberName;
    private final MemberRole role;
    /**
     * -- GETTER --
     *  <h3>사용자 토큰 ID 조회 (별칭 메서드)</h3>
     *  <p>authTokenId를 반환하는 편의 메서드</p>
     *
     * @return 토큰 ID
     *
     */
    private final Long authTokenId;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * <h3>기존 회원용 정적 팩토리 메서드</h3>
     * <p>
     * Member 엔티티로부터 CustomUserDetails 객체를 생성합니다.
     * JWT 토큰 생성 및 인증에 필요한 정보를 포함합니다.
     * </p>
     *
     * @param member Member 엔티티
     * @param authTokenId AuthToken ID
     * @return CustomUserDetails 객체
     * @author Jaeik
     * @since 3.0.0
     */
    public static CustomUserDetails ofExisting(Member member, Long authTokenId) {
        Long settingId = (member.getSetting() != null) ? member.getSetting().getId() : null;

        return CustomUserDetails.builder()
                .memberId(member.getId())
                .socialId(member.getSocialId())
                .provider(member.getProvider())
                .settingId(settingId)
                .socialNickname(member.getSocialNickname())
                .thumbnailImage(member.getThumbnailImage())
                .memberName(member.getMemberName())
                .role(member.getRole())
                .authTokenId(authTokenId)
                .authorities(createAuthorities(member.getRole()))
                .build();
    }

    /**
     * <h3>권한 생성</h3>
     *
     * <p>
     * 사용자 역할을 기반으로 권한을 생성한다.
     * </p>
     *
     * @param role 사용자 역할
     * @return 권한 컬렉션
     * @author Jaeik
     * @since 2.0.0
     */
    private static Collection<? extends GrantedAuthority> createAuthorities(MemberRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return authorities;
    }

    /**
     * <h3>사용자 소셜 제공자 조회 (별칭 메서드)</h3>
     *
     * <p>provider를 반환하는 편의 메서드</p>
     *
     * @return 소셜 제공자
     * @author Jaeik
     * @since 2.0.0
     */
    public SocialProvider getSocialProvider() {
        return provider;
    }

    /**
     * <h3>닉네임 조회</h3>
     *
     * <p>사용자의 닉네임을 반환한다.</p>
     *
     * @return 사용자 닉네임
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String getUsername() {
        return memberName;
    }

    /**
     * <h3>사용자 권한 조회</h3>
     * <p>사용자의 권한 목록을 반환합니다.</p>
     *
     * @return GrantedAuthority 컬렉션
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * <h3>사용자 비밀번호 조회</h3>
     * <p>현재 구현에서는 비밀번호를 사용하지 않으므로 null을 반환합니다.</p>
     *
     * @return 항상 null
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * <h3>계정 만료 여부 확인</h3>
     * <p>계정이 만료되지 않았는지 여부를 반환합니다. 항상 true를 반환합니다.</p>
     *
     * @return 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    /**
     * <h3>계정 잠금 여부 확인</h3>
     * <p>계정이 잠겨있지 않은지 여부를 반환합니다. 항상 true를 반환합니다.</p>
     *
     * @return 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    /**
     * <h3>자격 증명 만료 여부 확인</h3>
     * <p>자격 증명(비밀번호)이 만료되지 않았는지 여부를 반환합니다. 항상 true를 반환합니다.</p>
     *
     * @return 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    /**
     * <h3>계정 활성화 여부 확인</h3>
     * <p>계정이 활성화되어 있는지 여부를 반환합니다. 항상 true를 반환합니다.</p>
     *
     * @return 항상 true
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}