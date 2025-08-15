package jaeik.growfarm.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <h2>사용자 역할</h2>
 * <p>사용자의 역할을 정의하는 열거형</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
    WITHDRAWN("ROLE_WITHDRAWN");

    private final String value;
}
