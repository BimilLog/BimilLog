package jaeik.bimillog.domain.auth.event;

import java.time.LocalDateTime;

/**
 * <h2>사용자 로그아웃 이벤트</h2>
 * <p>사용자가 로그아웃할 때 발생하는 도메인 이벤트</p>
 * <p>이 이벤트를 통해 로그아웃과 관련된 부수적인 작업들(토큰 삭제, SSE 정리 등)을 분리하여 처리</p>
 *
 * @param userId      로그아웃한 사용자 ID
 * @param tokenId     사용자 토큰 ID
 * @param loggedOutAt 로그아웃 발생 시간
 * @author Jaeik
 * @version 2.0.0
 */
public record UserLoggedOutEvent(Long userId, Long tokenId, LocalDateTime loggedOutAt) {

    /**
     * <h3>사용자 로그아웃 이벤트 생성</h3>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return UserLoggedOutEvent 인스턴스
     * @since 2.0.0
     * @author Jaeik
     */
    public static UserLoggedOutEvent of(Long userId, Long tokenId) {
        return new UserLoggedOutEvent(userId, tokenId, LocalDateTime.now());
    }
}