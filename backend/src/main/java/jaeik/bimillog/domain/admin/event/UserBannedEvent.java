package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.listener.JwtBlacklistListener;
import jaeik.bimillog.infrastructure.adapter.in.user.listener.BlacklistAddListener;

/**
 * <h2>UserBannedEvent</h2>
 * <p>관리자가 대시보드에서 사용자 제재 결정을 내렸을 때 발생하는 도메인 이벤트입니다.</p>
 * <p>관리자가 신고를 검토한 후 제재 버튼을 클릭하면 AdminCommandService.banUser에서 이 이벤트를 발행합니다.</p>
 * <p>Auth 도메인의 UserBanListener가 이 이벤트를 구독하여 실제 사용자 계정 제재 로직을 실행합니다.</p>
 * <p>사용자의 소셜 로그인 정보를 포함하여 소셜 플랫폼에서의 접근 차단도 함께 처리할 수 있습니다.</p>
 * <p>{@link JwtBlacklistListener} JWT 토큰 무효화</p>
 * <p>{@link BlacklistAddListener} 사용자 차단 및 블랙리스트 등록</p>
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
        SocialProvider provider,
        String reason
) {}
