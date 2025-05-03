package jaeik.growfarm.global.jwt;

import jaeik.growfarm.dto.user.UserDTO;
import jaeik.growfarm.entity.user.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * CustomUserDetails 클래스
 * UserDetails 인터페이스를 구현하여 Spring Security에서 사용자 정보를 처리하는 클래스
 * UserDTO 객체를 사용하여 사용자 정보를 저장하고, 권한을 설정하는 기능을 제공
 * 수정일 : 2025-05-03
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UserDTO userDTO;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UserDTO userDTO) {
        this.userDTO = userDTO;
        this.authorities = createAuthorities(userDTO.getRole());
    }

    private Collection<? extends GrantedAuthority> createAuthorities(UserRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return authorities;
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
        return null;
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

    public Long getUserId() {
        return userDTO.getUserId();
    }

    public Long getTokenId() {
        return userDTO.getTokenId();
    }

    public String getFarmName() {
        return userDTO.getFarmName();
    }


}