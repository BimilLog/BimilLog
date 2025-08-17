package jaeik.growfarm.domain.notification.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationUrlGenerator 테스트</h2>
 * <p>알림 URL 생성기의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@DisplayName("NotificationUrlGenerator 테스트")
class NotificationUrlGeneratorTest {

    private NotificationUrlGenerator notificationUrlGenerator;

    private static final String BASE_URL = "https://bimillog.com";

    @BeforeEach
    void setUp() {
        notificationUrlGenerator = new NotificationUrlGenerator(BASE_URL);
    }

    @Test
    @DisplayName("게시물 URL 생성 - 성공")
    void shouldGeneratePostUrl_WhenValidPostId() {
        // Given
        Long postId = 123L;
        String expectedUrl = BASE_URL + "/board/post/123";

        // When
        String result = notificationUrlGenerator.generatePostUrl(postId);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("게시물 URL 생성 - 큰 ID 값")
    void shouldGeneratePostUrl_WhenLargePostId() {
        // Given
        Long postId = 999999999L;
        String expectedUrl = BASE_URL + "/board/post/999999999";

        // When
        String result = notificationUrlGenerator.generatePostUrl(postId);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("게시물 URL 생성 - ID가 0인 경우")
    void shouldGeneratePostUrl_WhenPostIdIsZero() {
        // Given
        Long postId = 0L;
        String expectedUrl = BASE_URL + "/board/post/0";

        // When
        String result = notificationUrlGenerator.generatePostUrl(postId);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 성공")
    void shouldGenerateRollingPaperUrl_WhenValidUserName() {
        // Given
        String userName = "testuser";
        String expectedUrl = BASE_URL + "/rolling-paper/testuser";

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 한글 사용자명")
    void shouldGenerateRollingPaperUrl_WhenKoreanUserName() {
        // Given
        String userName = "테스트사용자";
        String expectedUrl = BASE_URL + "/rolling-paper/테스트사용자";

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 특수문자 포함 사용자명")
    void shouldGenerateRollingPaperUrl_WhenUserNameWithSpecialChars() {
        // Given
        String userName = "test-user_123";
        String expectedUrl = BASE_URL + "/rolling-paper/test-user_123";

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 빈 사용자명")
    void shouldGenerateRollingPaperUrl_WhenEmptyUserName() {
        // Given
        String userName = "";
        String expectedUrl = BASE_URL + "/rolling-paper/";

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("페이퍼 URL 생성 (호환성) - 성공")
    void shouldGeneratePaperUrl_WhenValidUserName() {
        // Given
        String userName = "testuser";
        String expectedUrl = BASE_URL + "/rolling-paper/testuser";

        // When
        String result = notificationUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
        // generatePaperUrl과 generateRollingPaperUrl이 같은 결과를 반환하는지 확인
        assertThat(result).isEqualTo(notificationUrlGenerator.generateRollingPaperUrl(userName));
    }

    @Test
    @DisplayName("페이퍼 URL 생성 (호환성) - 한글 사용자명")
    void shouldGeneratePaperUrl_WhenKoreanUserName() {
        // Given
        String userName = "한글사용자";
        String expectedUrl = BASE_URL + "/rolling-paper/한글사용자";

        // When
        String result = notificationUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
        // generatePaperUrl과 generateRollingPaperUrl이 같은 결과를 반환하는지 확인
        assertThat(result).isEqualTo(notificationUrlGenerator.generateRollingPaperUrl(userName));
    }

    @Test
    @DisplayName("BaseUrl이 다른 경우 - 개발환경")
    void shouldGenerateUrls_WhenDifferentBaseUrl() {
        // Given
        String devBaseUrl = "http://localhost:3000";
        NotificationUrlGenerator devUrlGenerator = new NotificationUrlGenerator(devBaseUrl);
        Long postId = 456L;
        String userName = "devuser";

        // When
        String postUrl = devUrlGenerator.generatePostUrl(postId);
        String paperUrl = devUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(postUrl).isEqualTo("http://localhost:3000/board/post/456");
        assertThat(paperUrl).isEqualTo("http://localhost:3000/rolling-paper/devuser");
    }

    @Test
    @DisplayName("BaseUrl에 슬래시가 포함된 경우")
    void shouldGenerateUrls_WhenBaseUrlEndsWithSlash() {
        // Given
        String baseUrlWithSlash = "https://bimillog.com/";
        NotificationUrlGenerator urlGeneratorWithSlash = new NotificationUrlGenerator(baseUrlWithSlash);
        Long postId = 789L;
        String userName = "slashuser";

        // When
        String postUrl = urlGeneratorWithSlash.generatePostUrl(postId);
        String paperUrl = urlGeneratorWithSlash.generatePaperUrl(userName);

        // Then
        assertThat(postUrl).isEqualTo("https://bimillog.com//board/post/789");
        assertThat(paperUrl).isEqualTo("https://bimillog.com//rolling-paper/slashuser");
    }

    @Test
    @DisplayName("null 값 처리 - postId가 null인 경우")
    void shouldGeneratePostUrl_WhenPostIdIsNull() {
        // Given
        Long postId = null;
        String expectedUrl = BASE_URL + "/board/post/null";

        // When
        String result = notificationUrlGenerator.generatePostUrl(postId);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("null 값 처리 - userName이 null인 경우")
    void shouldGeneratePaperUrl_WhenUserNameIsNull() {
        // Given
        String userName = null;
        String expectedUrl = BASE_URL + "/rolling-paper/null";

        // When
        String result = notificationUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("게시물 URL 생성 - 음수 ID")
    void shouldGeneratePostUrl_WhenPostIdIsNegative() {
        // Given
        Long postId = -123L;
        String expectedUrl = BASE_URL + "/board/post/-123";

        // When
        String result = notificationUrlGenerator.generatePostUrl(postId);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 공백 사용자명")
    void shouldGenerateRollingPaperUrl_WhenUserNameWithSpaces() {
        // Given
        String userName = "user with spaces";
        String expectedUrl = BASE_URL + "/rolling-paper/user with spaces";

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("롤링페이퍼 URL 생성 - 매우 긴 사용자명")
    void shouldGenerateRollingPaperUrl_WhenVeryLongUserName() {
        // Given
        String veryLongUserName = "A".repeat(500);
        String expectedUrl = BASE_URL + "/rolling-paper/" + veryLongUserName;

        // When
        String result = notificationUrlGenerator.generateRollingPaperUrl(veryLongUserName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("URL 생성 성능 테스트")
    void shouldGenerateUrlsQuickly_WhenCalledMultipleTimes() {
        // Given
        int callCount = 1000;
        Long[] postIds = new Long[callCount];
        String[] userNames = new String[callCount];
        
        for (int i = 0; i < callCount; i++) {
            postIds[i] = (long) i;
            userNames[i] = "user" + i;
        }

        // When & Then
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < callCount; i++) {
            String postUrl = notificationUrlGenerator.generatePostUrl(postIds[i]);
            String paperUrl = notificationUrlGenerator.generatePaperUrl(userNames[i]);
            
            assertThat(postUrl).isNotNull().isNotEmpty();
            assertThat(paperUrl).isNotNull().isNotEmpty();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 1000번 호출이 1초 이내에 완료되어야 함
        assertThat(duration).isLessThan(1000L);
    }

    @Test
    @DisplayName("BaseUrl 검증 - null BaseUrl")
    void shouldHandleNullBaseUrl() {
        // Given
        String nullBaseUrl = null;
        NotificationUrlGenerator nullBaseUrlGenerator = new NotificationUrlGenerator(nullBaseUrl);
        Long postId = 123L;
        String userName = "testuser";

        // When
        String postUrl = nullBaseUrlGenerator.generatePostUrl(postId);
        String paperUrl = nullBaseUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(postUrl).isEqualTo("null/board/post/123");
        assertThat(paperUrl).isEqualTo("null/rolling-paper/testuser");
    }

    @Test
    @DisplayName("BaseUrl 검증 - 빈 BaseUrl")
    void shouldHandleEmptyBaseUrl() {
        // Given
        String emptyBaseUrl = "";
        NotificationUrlGenerator emptyBaseUrlGenerator = new NotificationUrlGenerator(emptyBaseUrl);
        Long postId = 456L;
        String userName = "emptyuser";

        // When
        String postUrl = emptyBaseUrlGenerator.generatePostUrl(postId);
        String paperUrl = emptyBaseUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(postUrl).isEqualTo("/board/post/456");
        assertThat(paperUrl).isEqualTo("/rolling-paper/emptyuser");
    }

    @Test
    @DisplayName("URL 일관성 검증 - 동일한 입력에 대해 동일한 결과")
    void shouldReturnConsistentResults_WhenSameInput() {
        // Given
        Long postId = 789L;
        String userName = "consistentuser";

        // When
        String postUrl1 = notificationUrlGenerator.generatePostUrl(postId);
        String postUrl2 = notificationUrlGenerator.generatePostUrl(postId);
        String paperUrl1 = notificationUrlGenerator.generatePaperUrl(userName);
        String paperUrl2 = notificationUrlGenerator.generatePaperUrl(userName);

        // Then
        assertThat(postUrl1).isEqualTo(postUrl2);
        assertThat(paperUrl1).isEqualTo(paperUrl2);
    }
}