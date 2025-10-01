package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.member.entity.member.MemberRole;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import jaeik.bimillog.domain.member.entity.memberdetail.ExistingMemberDetail;
import lombok.Getter;
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
 * ClientDTO 객체를 사용하여 사용자 정보를 저장하고, 권한을 설정하는 기능을 제공
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final ExistingMemberDetail existingMemberDetail;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * <h3>CustomUserDetails 생성자</h3>
     *
     * <p>
     * ClientDTO를 받아서 CustomUserDetails 객체를 생성한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param existingMemberDetail 사용자 정보 DTO
     */
    public CustomUserDetails(ExistingMemberDetail existingMemberDetail) {
        this.existingMemberDetail = existingMemberDetail;
        this.authorities = createAuthorities(existingMemberDetail.getRole());
    }

    /**
     * <h3>권한 생성</h3>
     *
     * <p>
     * 사용자 역할을 기반으로 권한을 생성한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param role 사용자 역할
     * @return 권한 컬렉션
     */
    private Collection<? extends GrantedAuthority> createAuthorities(MemberRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return authorities;
    }

    /**
     * <h3>사용자 ID 조회</h3>
     *
     * <p>
     * 사용자의 유저 ID를 반환한다.
     * </p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @return 유저 ID
     */
    public Long getMemberId() {
        return existingMemberDetail.getMemberId();
    }

    /**
     * <h3>사용자 토큰 ID 조회</h3>
     *
     * <p>사용자의 토큰 ID를 반환한다.</p>
     * @since 2.0.0
     * @author Jaeik
     * @return 토큰 ID
     */
    public Long getTokenId() {
        return existingMemberDetail.getTokenId();
    }

    /**
     * <h3>사용자 FCM 토큰 ID 조회</h3>
     *
     * <p>사용자의 FCM 토큰 ID를 반환한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return FCM 토큰 ID
     */
    public Long getFcmTokenId() {
        return existingMemberDetail.getFcmTokenId();
    }

    /**
     * <h3>사용자 설정 ID 조회</h3>
     *
     * <p>사용자의 설정 ID를 반환한다. JWT 토큰에서 직접 설정 조회를 한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 설정 ID
     */
    public Long getSettingId() {
        return existingMemberDetail.getSettingId();
    }

    /**
     * <h3>사용자 소셜 제공자 조회</h3>
     *
     * <p>사용자의 소셜 제공자를 반환한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 소셜 제공자
     */
    public SocialProvider getSocialProvider() {
        return existingMemberDetail.getProvider();
    }

    /**
     * <h3>사용자 소셜 ID 조회</h3>
     *
     * <p>사용자의 소셜 ID를 반환한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 소셜 ID
     */
    public String getSocialId() {return  existingMemberDetail.getSocialId();}

    /**
     * <h3>닉네임 조회</h3>
     *
     * <p>사용자의 닉네임을 반환한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 사용자 닉네임
     */
    @Override
    public String getUsername() {
        return existingMemberDetail.getMemberName();
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