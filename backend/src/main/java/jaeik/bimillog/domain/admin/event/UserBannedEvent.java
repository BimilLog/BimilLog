package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h2>UserBannedEvent</h2>
 * <p>
 * 관리자가 대시보드에서 사용자 제재 결정을 내렸을 때 발생하는 도메인 이벤트입니다.
 * </p>
 * <p>
 * 관리자가 신고를 검토한 후 제재 버튼을 클릭하면 AdminCommandService.banUser에서 이 이벤트를 발행합니다.
 * </p>
 * <p>
 * 이벤트 기반 아키텍처를 통해 Admin 도메인과 Auth 도메인 간의 결합도를 낮추면서
 * 사용자 제재 처리를 비동기적으로 수행할 수 있도록 합니다.
 * </p>
 * <p>
 * Auth 도메인의 UserBanListener가 이 이벤트를 구독하여 실제 사용자 계정 제재 로직을 실행합니다.
 * </p>
 * <p>
 * Record 클래스로 구현되어 불변성을 보장하고, 컴팩트 생성자를 통해 데이터 유효성 검증을 수행합니다.
 * </p>
 * <p>
 * 사용자의 소셜 로그인 정보를 포함하여 소셜 플랫폼에서의 접근 차단도 함께 처리할 수 있습니다.
 * </p>
 *
 * @param userId 제재 대상 사용자의 내부 시스템 ID
 * @param socialId 제재 대상 사용자의 소셜 플랫폼 고유 ID
 * @param provider 소셜 로그인 제공자 (KAKAO 등)
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
