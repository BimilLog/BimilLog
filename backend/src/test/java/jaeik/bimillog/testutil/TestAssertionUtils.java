package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.admin.exception.AdminCustomException;
import jaeik.bimillog.domain.admin.exception.AdminErrorCode;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.comment.exception.CommentErrorCode;
import jaeik.bimillog.domain.notification.exception.NotificationCustomException;
import jaeik.bimillog.domain.notification.exception.NotificationErrorCode;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>테스트 예외 검증 유틸리티</h2>
 * <p>테스트에서 반복되는 예외 검증 로직을 간소화하는 유틸리티</p>
 * <p>기존 3-4줄의 예외 검증 코드를 1줄로 단축</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestAssertionUtils {

    /**
     * Post 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertPostException(Runnable execution, PostErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", expectedErrorCode);
    }

    /**
     * Comment 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertCommentException(Runnable execution, CommentErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(CommentCustomException.class)
                .hasFieldOrPropertyWithValue("commentErrorCode", expectedErrorCode);
    }

    /**
     * Paper 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertPaperException(Runnable execution, PaperErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(PaperCustomException.class)
                .hasFieldOrPropertyWithValue("paperErrorCode", expectedErrorCode);
    }

    /**
     * User 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertUserException(Runnable execution, UserErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(UserCustomException.class)
                .hasFieldOrPropertyWithValue("userErrorCode", expectedErrorCode);
    }

    /**
     * Admin 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertAdminException(Runnable execution, AdminErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(AdminCustomException.class)
                .hasFieldOrPropertyWithValue("adminErrorCode", expectedErrorCode);
    }

    /**
     * Notification 도메인 예외 검증 (기존 3줄 → 1줄)
     */
    public static void assertNotificationException(Runnable execution, NotificationErrorCode expectedErrorCode) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(NotificationCustomException.class)
                .hasFieldOrPropertyWithValue("notificationErrorCode", expectedErrorCode);
    }

    /**
     * 일반적인 예외 클래스와 메시지 검증 (기존 2-3줄 → 1줄)
     */
    public static void assertException(Runnable execution, Class<? extends Exception> exceptionClass, String expectedMessage) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(exceptionClass)
                .hasMessage(expectedMessage);
    }

    /**
     * 예외 클래스만 검증 (메시지 무시)
     */
    public static void assertExceptionType(Runnable execution, Class<? extends Exception> exceptionClass) {
        assertThatThrownBy(execution::run)
                .isInstanceOf(exceptionClass);
    }

    /**
     * NullPointerException 검증 단축
     */
    public static void assertNullPointerException(Runnable execution) {
        assertExceptionType(execution, NullPointerException.class);
    }

    /**
     * IllegalArgumentException 검증 단축
     */
    public static void assertIllegalArgumentException(Runnable execution) {
        assertExceptionType(execution, IllegalArgumentException.class);
    }

    /**
     * RuntimeException과 메시지 검증 단축
     */
    public static void assertRuntimeException(Runnable execution, String expectedMessage) {
        assertException(execution, RuntimeException.class, expectedMessage);
    }

    // Private constructor to prevent instantiation
    private TestAssertionUtils() {}
}