package jaeik.growfarm.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>UserSignedUpEvent</h2>
 * <p>
 *     사용자 회원가입 시 발생하는 이벤트입니다.
 * </p>
 * @author jaeik
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class UserSignedUpEvent {

    private final Long userId;
}
