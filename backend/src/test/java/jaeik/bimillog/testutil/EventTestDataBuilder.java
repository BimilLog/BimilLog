package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.admin.entity.ReportType;
import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.admin.event.UserBannedEvent;
import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.event.ReportSubmittedEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // ==================== Report Events ====================

    /**
     * 신고 제출 이벤트 생성
     * @param reporterId 신고자 ID
     * @param reporterName 신고자 이름
     * @param reportType 신고 타입
     * @param targetId 대상 ID
     * @param content 신고 내용
     * @return ReportSubmittedEvent
     */
    public static ReportSubmittedEvent createReportEvent(
            Long reporterId, String reporterName, ReportType reportType,
            Long targetId, String content) {
        return ReportSubmittedEvent.of(reporterId, reporterName, reportType, targetId, content);
    }

    /**
     * 댓글 신고 이벤트 생성 (인증된 사용자)
     * @param reporterId 신고자 ID
     * @param targetId 댓글 ID
     * @return ReportSubmittedEvent
     */
    public static ReportSubmittedEvent createCommentReportEvent(Long reporterId, Long targetId) {
        return createReportEvent(reporterId, "testuser", ReportType.COMMENT,
                targetId, "부적절한 댓글입니다");
    }

    /**
     * 게시글 신고 이벤트 생성 (익명 사용자)
     * @param targetId 게시글 ID
     * @return ReportSubmittedEvent
     */
    public static ReportSubmittedEvent createAnonymousPostReportEvent(Long targetId) {
        return createReportEvent(null, "익명", ReportType.POST,
                targetId, "스팸 게시글입니다");
    }

    /**
     * 건의사항 이벤트 생성
     * @param reporterId 신고자 ID
     * @param content 건의 내용
     * @return ReportSubmittedEvent
     */
    public static ReportSubmittedEvent createImprovementEvent(Long reporterId, String content) {
        return createReportEvent(reporterId, "suggester", ReportType.IMPROVEMENT,
                null, content);
    }

    /**
     * 여러 신고 이벤트 생성
     * @return List of ReportSubmittedEvent
     */
    public static List<ReportSubmittedEvent> createMultipleReportEvents() {
        List<ReportSubmittedEvent> events = new ArrayList<>();
        events.add(createCommentReportEvent(1L, 100L));
        events.add(createAnonymousPostReportEvent(200L));
        events.add(createImprovementEvent(3L, "새로운 기능을 건의합니다"));
        return events;
    }

    // ==================== Comment Events ====================

    /**
     * 댓글 생성 이벤트 생성
     * @param postUserId 게시글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param postId 게시글 ID
     * @return CommentCreatedEvent
     */
    public static CommentCreatedEvent createCommentEvent(
            Long postUserId, String commenterName, Long postId) {
        return new CommentCreatedEvent(postUserId, commenterName, postId);
    }

    /**
     * 기본 댓글 생성 이벤트
     * @param postId 게시글 ID
     * @return CommentCreatedEvent
     */
    public static CommentCreatedEvent createDefaultCommentEvent(Long postId) {
        return createCommentEvent(1L, "댓글작성자", postId);
    }

    /**
     * 여러 댓글 이벤트 생성 (동일 게시글)
     * @param postUserId 게시글 작성자 ID
     * @param postId 게시글 ID
     * @param count 댓글 수
     * @return List of CommentCreatedEvent
     */
    public static List<CommentCreatedEvent> createMultipleCommentsForPost(
            Long postUserId, Long postId, int count) {
        List<CommentCreatedEvent> events = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            events.add(createCommentEvent(postUserId, "댓글작성자" + i, postId));
        }
        return events;
    }

    /**
     * 여러 댓글 이벤트 생성 (다른 게시글)
     * @param count 게시글 수
     * @return List of CommentCreatedEvent
     */
    public static List<CommentCreatedEvent> createCommentsForDifferentPosts(int count) {
        List<CommentCreatedEvent> events = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            events.add(createCommentEvent((long) i, "댓글러" + i, 100L + i));
        }
        return events;
    }

    // ==================== Post Events ====================

    /**
     * 게시글 조회 이벤트 생성
     * @param postId 게시글 ID
     * @return PostViewedEvent
     */
    public static PostViewedEvent createPostViewEvent(Long postId) {
        return new PostViewedEvent(postId);
    }

    /**
     * 여러 게시글 조회 이벤트 생성
     * @param postIds 게시글 ID 배열
     * @return List of PostViewedEvent
     */
    public static List<PostViewedEvent> createMultipleViewEvents(Long... postIds) {
        List<PostViewedEvent> events = new ArrayList<>();
        for (Long postId : postIds) {
            events.add(createPostViewEvent(postId));
        }
        return events;
    }

    /**
     * 동일 게시글 반복 조회 이벤트 생성
     * @param postId 게시글 ID
     * @param count 조회 횟수
     * @return List of PostViewedEvent
     */
    public static List<PostViewedEvent> createRepeatedViewEvents(Long postId, int count) {
        List<PostViewedEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createPostViewEvent(postId));
        }
        return events;
    }

    /**
     * 게시글 인기글 등극 이벤트 생성
     * @param userId 사용자 ID
     * @param sseMessage SSE 메시지
     * @param postId 게시글 ID
     * @param fcmTitle FCM 제목
     * @param fcmBody FCM 내용
     * @return PostFeaturedEvent
     */
    public static PostFeaturedEvent createPostFeaturedEvent(Long userId, String sseMessage, Long postId, String fcmTitle, String fcmBody) {
        return new PostFeaturedEvent(userId, sseMessage, postId, fcmTitle, fcmBody);
    }

    // ==================== Auth Events ====================

    /**
     * 사용자 로그아웃 이벤트 생성
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @param loggedOutAt 로그아웃 시간
     * @return UserLoggedOutEvent
     */
    public static UserLoggedOutEvent createLogoutEvent(Long userId, Long tokenId, LocalDateTime loggedOutAt) {
        return new UserLoggedOutEvent(userId, tokenId, loggedOutAt);
    }

    /**
     * 기본 사용자 로그아웃 이벤트
     * @param userId 사용자 ID
     * @return UserLoggedOutEvent
     */
    public static UserLoggedOutEvent createDefaultLogoutEvent(Long userId) {
        return UserLoggedOutEvent.of(userId, 100L + userId);
    }

    /**
     * 사용자 탈퇴 이벤트 생성
     * @param userId 사용자 ID
     * @param socialId 소셜 ID
     * @param provider 소셜 제공자
     * @return UserWithdrawnEvent
     */
    public static UserWithdrawnEvent createWithdrawEvent(Long userId, String socialId, SocialProvider provider) {
        return new UserWithdrawnEvent(userId, socialId, provider);
    }

    /**
     * 기본 사용자 탈퇴 이벤트
     * @param userId 사용자 ID
     * @return UserWithdrawnEvent
     */
    public static UserWithdrawnEvent createDefaultWithdrawEvent(Long userId) {
        return createWithdrawEvent(userId, "testSocialId" + userId, SocialProvider.KAKAO);
    }


    // ==================== Paper Events ====================

    /**
     * 롤링페이퍼 이벤트 생성
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
     * @param userId 차단된 사용자 ID
     * @param socialId 소셜 ID
     * @param provider 소셜 제공자
     * @return UserBannedEvent
     */
    public static UserBannedEvent createUserBannedEvent(Long userId, String socialId, SocialProvider provider) {
        return new UserBannedEvent(userId, socialId, provider);
    }

    /**
     * 관리자 강제 탈퇴 이벤트 생성
     * @param userId 사용자 ID
     * @param reason 탈퇴 사유
     * @return AdminWithdrawEvent
     */
    public static AdminWithdrawEvent createAdminWithdrawEvent(Long userId, String reason) {
        return new AdminWithdrawEvent(userId, reason);
    }

    // ==================== Mixed Event Scenarios ====================

    // Private constructor to prevent instantiation
    private EventTestDataBuilder() {}
}