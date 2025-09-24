package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.user.entity.SocialProvider;

import java.time.LocalDateTime;

/**
 * <h2>Event 테스트 데이터 빌더</h2>
 * <p>각 도메인 이벤트를 쉽게 생성하기 위한 빌더 클래스</p>
 * <p>자주 사용되는 테스트 시나리오에 대한 팩터리 메서드 제공</p>
 *
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>각 이벤트 타입별 생성 메서드</li>
 *   <li>기본값을 사용한 간단한 이벤트 생성</li>
 *   <li>다중 이벤트 생성 유틸리티</li>
 *   <li>일반적인 테스트 시나리오 템플릿</li>
 * </ul>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public class EventTestDataBuilder {

    // ==================== Post Events ====================


    /**
     * 게시글 인기글 등극 이벤트 생성
     *
     * @param userId     사용자 ID
     * @param sseMessage SSE 메시지
     * @param postId     게시글 ID
     * @param fcmTitle   FCM 제목
     * @param fcmBody    FCM 내용
     * @return PostFeaturedEvent
     */
    public static PostFeaturedEvent createPostFeaturedEvent(Long userId, String sseMessage, Long postId, String fcmTitle, String fcmBody) {
        return new PostFeaturedEvent(userId, sseMessage, postId, fcmTitle, fcmBody);
    }

    // ==================== Auth Events ====================

    /**
     * 사용자 로그아웃 이벤트 생성
     *
     * @param userId      사용자 ID
     * @param tokenId     토큰 ID
     * @param loggedOutAt 로그아웃 시간
     * @return UserLoggedOutEvent
     */
    public static UserLoggedOutEvent createLogoutEvent(Long userId, Long tokenId, LocalDateTime loggedOutAt) {
        return new UserLoggedOutEvent(userId, tokenId, loggedOutAt);
    }

    /**
     * 기본 사용자 로그아웃 이벤트
     *
     * @param userId 사용자 ID
     * @return UserLoggedOutEvent
     */
    public static UserLoggedOutEvent createDefaultLogoutEvent(Long userId) {
        return UserLoggedOutEvent.of(userId, 100L + userId);
    }

    /**
     * 사용자 탈퇴 이벤트 생성
     *
     * @param userId   사용자 ID
     * @param socialId 소셜 ID
     * @param provider 소셜 제공자
     * @return UserWithdrawnEvent
     */
    public static UserWithdrawnEvent createWithdrawEvent(Long userId, String socialId, SocialProvider provider) {
        return new UserWithdrawnEvent(userId, socialId, provider);
    }

    /**
     * 기본 사용자 탈퇴 이벤트
     *
     * @param userId 사용자 ID
     * @return UserWithdrawnEvent
     */
    public static UserWithdrawnEvent createDefaultWithdrawEvent(Long userId) {
        return createWithdrawEvent(userId, "testSocialId" + userId, SocialProvider.KAKAO);
    }


    // ==================== Paper Events ====================

    /**
     * 롤링페이퍼 이벤트 생성
     *
     * @param receiverId 수신자 ID
     * @param senderName 발신자 이름
     * @return RollingPaperEvent
     */
    public static RollingPaperEvent createPaperEvent(Long receiverId, String senderName) {
        return new RollingPaperEvent(receiverId, senderName);
    }

    // ==================== Admin Events ====================

    /**
     * 사용자 차단 이벤트 생성
     *
     * @param userId   차단된 사용자 ID
     * @param socialId 소셜 ID
     * @param provider 소셜 제공자
     * @return UserBannedEvent
     */
    public static UserBannedEvent createUserBannedEvent(Long userId, String socialId, SocialProvider provider) {
        return new UserBannedEvent(userId, socialId, provider);
    }

    /**
     * 관리자 강제 탈퇴 이벤트 생성
     *
     * @param userId 사용자 ID
     * @param reason 탈퇴 사유
     * @return AdminWithdrawEvent
     */
    public static AdminWithdrawEvent createAdminWithdrawEvent(Long userId, String reason) {
        return new AdminWithdrawEvent(userId, reason);
    }
}