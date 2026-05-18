package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.member.entity.SocialProvider;

/**
 * <h2>MemberBannedEvent</h2>
 * <p>블랙리스트에 등록하여 사용자를 일정 기간 차단합니다.</p>
 * <p>AdminCommandService.banUser에서 이 이벤트를 발행합니다.</p>
 *
 * @param memberId 제재 대상 사용자의 ID
 * @param socialId 제재 대상 사용자의 소셜 플랫폼 고유 ID
 * @param provider 소셜 로그인 제공자 (KAKAO 등)
 * @author Jaeik
 * @version 2.0.0
 */
public record MemberBannedEvent(Long memberId, String socialId, SocialProvider provider) {}
