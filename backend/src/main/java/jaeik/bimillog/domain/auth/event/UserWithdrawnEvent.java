package jaeik.bimillog.domain.auth.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.auth.in.listener.SocialUnlinkListener;
import jaeik.bimillog.infrastructure.adapter.comment.in.listener.CommentRemoveListener;
import jaeik.bimillog.infrastructure.adapter.notification.in.listener.FcmTokenRemoveListener;

/**
 * <h2>사용자 회원탈퇴 이벤트</h2>
 * <p>사용자의 회원 탈퇴 시 발생하는 도메인 이벤트입니다.</p>
 * <p>소셜 계정 연동 해제, 관련 데이터 정리, 알림 구독 해제 등의 작업 트리거</p>
 *
 * @param userId 탈퇴한 사용자의 고유 ID
 * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
 * @param provider 연동된 소셜 플랫폼 제공자
 * <p>{@link  CommentRemoveListener} 댓글 데이터 정리 (소프트/하드 삭제)</p>
 * {@link  FcmTokenRemoveListener} FCM 토큰 제거
 * {@link  SocialUnlinkListener} 소셜 로그인 연결 해제
 * @author Jaeik
 * @version 2.0.0
 */
public record UserWithdrawnEvent(Long userId,
                                 String socialId,
                                 SocialProvider provider) {
}
