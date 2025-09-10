package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.domain.auth.application.service.LogoutService;

import java.time.LocalDateTime;

/**
 * <h2>사용자 로그아웃 이벤트</h2>
 * <p>사용자 로그아웃 시 발생하는 도메인 이벤트입니다.</p>
 * <p>토큰 무효화, SSE 연결 정리, 알림 구독 해제 등의 후처리 작업 트리거</p>
 *
 * @param userId 로그아웃을 수행한 사용자의 고유 ID
 * @param tokenId 무효화할 JWT 토큰의 고유 ID
 * @param loggedOutAt 로그아웃 이벤트가 발생한 정확한 시간
 * @author Jaeik
 * @version 2.0.0
 */
public record UserLoggedOutEvent(Long userId, Long tokenId, LocalDateTime loggedOutAt) {

    /**
     * <h3>로그아웃 이벤트 생성</h3>
     * <p>현재 시간으로 로그아웃 타임스탬프를 설정하여 이벤트를 생성합니다.</p>
     * <p>{@link LogoutService}에서 사용자 로그아웃 처리 후 후속 작업 트리거를 위해 호출됩니다.</p>
     *
     * @param userId 로그아웃한 사용자의 고유 식별자
     * @param tokenId 무효화할 토큰의 고유 식별자
     * @return 현재 시간으로 생성된 UserLoggedOutEvent 인스턴스
     * @author Jaeik
     * @since 2.0.0
     */
    public static UserLoggedOutEvent of(Long userId, Long tokenId) {
        return new UserLoggedOutEvent(userId, tokenId, LocalDateTime.now());
    }
}