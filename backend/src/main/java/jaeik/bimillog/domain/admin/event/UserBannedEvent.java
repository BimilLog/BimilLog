package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>사용자 차단 이벤트</h2>
 * <p>사용자가 관리자에 의해 차단되었을 때 발생하는 이벤트</p>
 *
 * @param userId 차단된 사용자 ID
 * @param socialId 차단된 사용자 소셜 ID
 * @param provider 소셜 제공자
 * @author Jaeik
 * @version 2.0.0
 */
public record UserBannedEvent(
        Long userId,
        String socialId,
        SocialProvider provider
) {
    public UserBannedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        if (socialId == null || socialId.isBlank()) {
            throw new IllegalArgumentException("소셜 ID는 null이거나 비어있을 수 없습니다.");
        }
        if (provider == null) {
            throw new IllegalArgumentException("소셜 제공자는 null일 수 없습니다.");
        }
    }
}
