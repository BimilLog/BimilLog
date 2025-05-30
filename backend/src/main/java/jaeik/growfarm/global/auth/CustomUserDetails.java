package jaeik.growfarm.global.auth;

import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <h3>커스텀 사용자 상세 정보 클래스</h3>
 * <p>
 * UserDetails 인터페이스를 구현하여 Spring Security에서 사용자 정보를 처리하는 클래스
 * </p>
 * <p>
 * UserDTO 객체를 사용하여 사용자 정보를 저장하고, 권한을 설정하는 기능을 제공
 * </p>
 * 
 * @since 1.0.0
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
     * UserDTO를 받아 CustomUserDetails 객체를 생성한다.
     * </p>
     * 
     * @since 1.0.0
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
     * @since 1.0.0
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
     * 사용자의 ID를 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 사용자 ID
     */
    public Long getUserId() {
        return userDTO.getUserId();
    }

    /**
     * <h3>토큰 ID 조회</h3>
     *
     * <p>
     * 사용자의 토큰 ID를 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @return 토큰 ID
     */
    public Long getTokenId() {
        return userDTO.getTokenId();
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
    public String getUsername() {
        return userDTO.getFarmName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}