package jaeik.bimillog.domain.user.event;

import jaeik.bimillog.domain.user.entity.user.SocialProvider;

/**
 * <h2>사용자 회원탈퇴 이벤트</h2>
 * <p>사용자의 회원 탈퇴 시 발생하는 도메인 이벤트입니다.</p>
 * <p>소셜 계정 연동 해제, 관련 데이터 정리, 알림 구독 해제 등의 작업 트리거</p>
 *
 * @param userId 탈퇴하는 사용자의 고유 ID
 * @param socialId 사용자의 소셜 플랫폼 고유 ID
 * @param provider 소셜 로그인 제공자 (KAKAO 등)
 * @author Jaeik
 * @version 2.0.0
 */
public record UserWithdrawnEvent(Long userId,
                                 String socialId,
                                 SocialProvider provider) {
}
