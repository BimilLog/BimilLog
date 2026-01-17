package jaeik.bimillog.domain.global.listener;

import jaeik.bimillog.domain.admin.service.AdminCommandService;
import jaeik.bimillog.domain.auth.service.AuthTokenService;
import jaeik.bimillog.domain.auth.service.SocialTokenService;
import jaeik.bimillog.domain.auth.service.SocialWithdrawService;
import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.domain.member.event.MemberWithdrawnEvent;
import jaeik.bimillog.domain.member.service.MemberAccountService;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.SseService;
import jaeik.bimillog.domain.paper.service.PaperCommandService;
import jaeik.bimillog.domain.post.service.PostCommandService;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.infrastructure.log.Log;

/**
 * <h2>사용자 탈퇴 이벤트 리스너</h2>
 * <p>사용자 탈퇴 또는 강제 탈퇴 시 발생하는 {@link MemberWithdrawnEvent}를 비동기로 처리합니다.</p>
 * <p>소셜 계정 연동 해제, 댓글/게시글 삭제, 토큰 무효화, 알림/메시지/신고 기록 삭제, 계정 정보 삭제를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Log(logResult = false, message = "회원 탈퇴 이벤트")
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawListener {
    private final SocialWithdrawService socialWithdrawService;
    private final SseService sseService;
    private final NotificationCommandService notificationCommandUseCase;
    private final CommentCommandService commentCommandService;
    private final PostCommandService postCommandService;
    private final AuthTokenService authTokenService;
    private final PaperCommandService paperCommandService;
    private final AdminCommandService adminCommandService;
    private final MemberAccountService memberAccountService;
    private final SocialTokenService socialTokenService;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final RedisFriendshipRepository redisFriendshipRepository;

    /**
     * <h3>사용자 탈퇴 이벤트 처리</h3>
     * <p>사용자가 회원 탈퇴하거나 관리자에 의해 강제 탈퇴될 때 발생하는 이벤트를 처리합니다.</p>
     * <p>모든 관련 데이터를 순차적으로 정리합니다: SSE 연결, 소셜 계정 연동 해제, 댓글 처리, 게시글 삭제,
     * 토큰 무효화, FCM 토큰 삭제, 알림 삭제, 롤링페이퍼 메시지 삭제, 소셜 토큰 삭제, 계정 정보 삭제</p>
     * <p>신고 기록은 익명화</p>
     * @param userWithdrawnEvent 회원 탈퇴 이벤트 (memberId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void memberWithdraw(MemberWithdrawnEvent userWithdrawnEvent) {
        Long memberId = userWithdrawnEvent.memberId();
        String socialId = userWithdrawnEvent.socialId();
        SocialProvider provider = userWithdrawnEvent.provider();

        // SSE 연결해제
        sseService.deleteEmitters(memberId, null);

        // 소셜 계정 연결해제
        try {
            socialWithdrawService.unlinkSocialAccount(provider, socialId, memberId);
        } catch (Exception ex) {
            log.warn("소셜 계정 연동 해제 실패 - provider: {}, socialId: {}. 탈퇴 후속 처리를 계속 진행합니다.", provider, socialId, ex);
        }

        // 게시글 삭제, 게시글 삭제 시 댓글과 각 추천이 함께 제거되고, 타 게시글에 남긴 댓글은 별도 처리합니다.
        // 타인의 글과 댓글에 있는 추천은 회원 탈퇴시에 처리 됩니다.
        postCommandService.deleteAllPostsByMemberId(memberId);

        // 타 게시글의 댓글 삭제 및 익명화
        commentCommandService.processUserCommentsOnWithdrawal(memberId);

        // 롤링페이퍼 메시지 삭제
        paperCommandService.deleteMessageInMyPaper(memberId, null);

        // 알림 삭제
        notificationCommandUseCase.deleteAllNotification(memberId);

        // 모든 FCM 토큰은 DB레벨 CasCade로 member 삭제 시 동시 삭제
        // 모든 AuthToken 제거
        authTokenService.deleteTokens(memberId, null);

        // 신고자 익명화
        adminCommandService.anonymizeReporterByUserId(memberId);

        // 소셜 토큰 제거
        socialTokenService.deleteByMemberId(memberId);

        // Redis 상호작용 테이블 정리 (SCAN 패턴 매칭 사용)
        try {
            redisInteractionScoreRepository.deleteInteractionKeyByWithdraw(memberId);
            log.debug("Redis 상호작용 테이블 정리 완료: memberId={}", memberId);
        } catch (Exception e) {
            log.error("Redis 상호작용 테이블 정리 실패: memberId={}. 탈퇴 후속 처리를 계속 진행합니다.", memberId, e);
        }

        // Redis 친구 관계 테이블 정리 (SCAN 패턴 매칭 사용)
        try {
            redisFriendshipRepository.deleteWithdrawFriendTargeted(memberId);
            log.debug("Redis 친구 관계 테이블 정리 완료: memberId={}", memberId);
        } catch (Exception e) {
            log.error("Redis 친구 관계 테이블 정리 실패: memberId={}. 탈퇴 후속 처리를 계속 진행합니다.", memberId, e);
        }

        // 사용자 정보 삭제 Cascade로 설정도 함께 삭제
        memberAccountService.removeMemberAccount(memberId);
    }
}
