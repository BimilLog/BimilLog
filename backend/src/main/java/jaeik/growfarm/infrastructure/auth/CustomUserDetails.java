package jaeik.growfarm.infrastructure.auth;

import jaeik.growfarm.global.domain.UserRole;
import jaeik.growfarm.dto.user.UserDTO;
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
 * @since 2.0.0
 * @author Jaeik
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UserDTO userDTO;
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
     * @param userDTO 사용자 정보 DTO
     */
    public CustomUserDetails(UserDTO userDTO) {
        this.userDTO = userDTO;
        this.authorities = createAuthorities(userDTO.getRole());
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
    private Collection<? extends GrantedAuthority> createAuthorities(UserRole role) {
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
    public Long getUserId() {
        return userDTO.getUserId();
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
        return userDTO.getTokenId();
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
        return userDTO.getFcmTokenId();
    }

    /**
     * <h3>사용자 설정 ID 조회</h3>
     *
     * <p>사용자의 설정 ID를 반환한다. JWT 토큰에서 직접 활용하여 효율적인 설정 조회를 가능하게 한다.</p>
     *
     * @since 2.0.0
     * @author Jaeik
     * @return 설정 ID
     */
    public Long getSettingId() {
        return userDTO.getSettingId();
    }

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
        return userDTO.getUserName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}