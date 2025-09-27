package jaeik.bimillog.domain.admin.event;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.infrastructure.adapter.in.auth.listener.JwtBlacklistListener;
import jaeik.bimillog.infrastructure.adapter.in.comment.listener.CommentRemoveListener;
import jaeik.bimillog.infrastructure.adapter.in.user.listener.BlacklistAddListener;

/**
 * <h2>UserForcedWithdrawalEvent</h2>
 * <p>사용자 강제 탈퇴 시 발생하는 이벤트입니다.</p>
 * <p>AdminCommandService.forceWithdrawUser에서 이 이벤트를 발행합니다.</p>
 * <p>단순 제재와 달리 사용자의 모든 데이터를 정리하고 재가입을 차단하는 강력한 최종 조치입니다.</p>
 * <p>Auth 도메인의 AdminWithdrawListener가 이 이벤트를 구독하여 실제 강제 탈퇴 로직을 실행하고,
 * Comment 도메인에서도 해당 사용자의 댓글 정리 작업을 수행합니다.
 * </p>
 * <p>{@link JwtBlacklistListener} JWT 토큰 무효화</p>
 * <p>{@link CommentRemoveListener} 댓글 데이터 정리</p>
 * <p>{@link BlacklistAddListener} 블랙리스트 등록</p>
 *
 * @param userId 강제 탈퇴 대상 사용자의 내부 시스템 ID
 * @author Jaeik
 * @version 2.0.0
 */
public record UserForcedWithdrawalEvent(
        Long userId,
        String socialId,
        SocialProvider provider,
        String reason
) {}