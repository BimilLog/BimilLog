package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.domain.member.entity.SocialProvider;

/**
 * <h2>사용자 로그아웃 이벤트</h2>
 * <p>사용자 로그아웃 시 발생하는 도메인 이벤트입니다.</p>
 * <p>토큰 무효화, SSE 연결 정리, 알림 구독 해제 등의 후처리 작업 트리거</p>
 * <p>AuthToken 삭제 시 fcmRegistrationToken도 함께 삭제됩니다 (테이블 통합)</p>
 *
 * @param memberId 로그아웃을 수행한 사용자의 고유 ID
 * @param authTokenId 무효화할 JWT 토큰의 고유 ID (AuthToken 삭제 시 FCM 토큰도 함께 삭제)
 * @param provider 소셜 로그인 제공자 (KAKAO 등)
 * @author Jaeik
 * @version 2.0.0
 *
 */
public record MemberLoggedOutEvent(Long memberId,
                                   Long authTokenId,
                                   SocialProvider provider){

}