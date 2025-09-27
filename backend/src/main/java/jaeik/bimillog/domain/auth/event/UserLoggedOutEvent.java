package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;

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
 *
 */
public record UserLoggedOutEvent(CustomUserDetails userDetails){

}