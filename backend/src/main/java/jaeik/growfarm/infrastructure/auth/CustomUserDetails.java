package jaeik.growfarm.infrastructure.auth;

import jaeik.growfarm.domain.user.domain.UserRole;
import jaeik.growfarm.dto.user.ClientDTO;
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

    private final ClientDTO clientDTO;
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
     * @param clientDTO 사용자 정보 DTO
     */
    public CustomUserDetails(ClientDTO clientDTO) {
        this.clientDTO = clientDTO;
        this.authorities = createAuthorities(clientDTO.getRole());
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
        return clientDTO.getUserId();
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
        return clientDTO.getTokenId();
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
        return clientDTO.getFcmTokenId();
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
        return clientDTO.getUserName();
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