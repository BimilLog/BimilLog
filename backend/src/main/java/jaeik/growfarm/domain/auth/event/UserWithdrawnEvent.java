package jaeik.growfarm.domain.auth.event;

/**
 * <h2>UserWithdrawnEvent</h2>
 * <p>
 * 사용자 회원 탈퇴 시 발생하는 이벤트입니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
public record UserWithdrawnEvent(Long userId) {

}
