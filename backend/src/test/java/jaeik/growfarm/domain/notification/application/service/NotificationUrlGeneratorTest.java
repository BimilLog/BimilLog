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
}