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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jaeik.bimillog.infrastructure.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static jaeik.bimillog.infrastructure.redis.RedisKey.PIPELINE_BATCH_SIZE;

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
    private final NotificationCommandService notificationCommandService;
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
    @Async("memberEventExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {
                    TransientDataAccessException.class,
                    DataAccessResourceFailureException.class,
                    RedisConnectionFailureException.class,
                    QueryTimeoutException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public void memberWithdraw(MemberWithdrawnEvent userWithdrawnEvent) {
        Long memberId = userWithdrawnEvent.memberId();
        String socialId = userWithdrawnEvent.socialId();
        SocialProvider provider = userWithdrawnEvent.provider();

        sseService.deleteEmitters(memberId, null); // SSE 연결해제

        try {
            socialWithdrawService.unlinkSocialAccount(provider, socialId, memberId); // 소셜 계정 연결해제
        } catch (Exception ex) {
            log.warn("소셜 계정 연동 해제 실패 - provider: {}, socialId: {}. 탈퇴 후속 처리를 계속 진행합니다.", provider, socialId, ex);
        }

        // 게시글 삭제, 게시글 삭제 시 댓글과 각 추천이 함께 제거되고, 타 게시글에 남긴 댓글은 별도 처리합니다.
        // 타인의 글과 댓글에 있는 추천은 회원 탈퇴시에 처리 됩니다.
        postCommandService.deleteAllPostsByMemberId(memberId);
        commentCommandService.processUserCommentsOnWithdrawal(memberId); // 타 게시글의 댓글 삭제 및 익명화
        paperCommandService.deleteAllMessageWhenWithdraw(memberId); // 롤링페이퍼 메시지 삭제
        notificationCommandService.deleteAllNotification(memberId); // 알림 삭제
        authTokenService.deleteTokens(memberId, null); // 모든 AuthToken 제거
        adminCommandService.anonymizeReporterByUserId(memberId); // 신고자 익명화
        socialTokenService.deleteByMemberId(memberId); // 소셜 토큰 제거

        // Redis 상호작용 테이블 정리
        try {
            redisInteractionScoreRepository.deleteInteractionKeyByWithdraw(memberId);
        } catch (Exception e) {
            log.error("Redis 상호작용 테이블 정리 실패: memberId={}. 탈퇴 후속 처리를 계속 진행합니다.", memberId, e);
        }

        // Redis 친구 관계 테이블 정리
        try {
            Set<Long> friendIds = redisFriendshipRepository.getFriendIdRandom(memberId, PIPELINE_BATCH_SIZE);
            List<Long> friendIdList = new ArrayList<>(friendIds);
            redisFriendshipRepository.deleteWithdrawFriendTargeted(friendIdList, memberId);
        } catch (Exception e) {
            log.error("Redis 친구 관계 테이블 정리 실패: memberId={}. 탈퇴 후속 처리를 계속 진행합니다.", memberId, e);
        }

        // 사용자 정보 삭제 Cascade로 설정도 함께 삭제 모든 FCM 토큰은 DB레벨 CasCade로 동시 삭제
        memberAccountService.removeMemberAccount(memberId);
    }

    /**
     * <h3>회원 탈퇴 처리 최종 실패 복구</h3>
     * <p>모든 재시도가 실패한 후 호출됩니다.</p>
     *
     * @param e 발생한 예외
     * @param event 회원 탈퇴 이벤트
     */
    @Recover
    public void recoverMemberWithdraw(Exception e, MemberWithdrawnEvent event) {
        log.error("회원 탈퇴 처리 최종 실패: memberId={}", event.memberId(), e);
    }
}
