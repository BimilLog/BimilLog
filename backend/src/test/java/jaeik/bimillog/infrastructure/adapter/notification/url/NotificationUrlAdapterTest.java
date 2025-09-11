package jaeik.bimillog.infrastructure.adapter.notification.url;

import jaeik.bimillog.infrastructure.adapter.notification.out.url.NotificationUrlAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>NotificationUrlAdapter 테스트</h2>
 * <p>알림 URL 생성 어댑터의 기능을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@DisplayName("NotificationUrlAdapter 테스트")
class NotificationUrlAdapterTest {

    private NotificationUrlAdapter notificationUrlAdapter;

    private static final String BASE_URL = "https://bimillog.com";

    @BeforeEach
    void setUp() {
        notificationUrlAdapter = new NotificationUrlAdapter(BASE_URL);
    }

    @Test
    @DisplayName("게시물 URL 생성 - 성공")
    void shouldGeneratePostUrl_WhenValidPostId() {
        // Given
        Long postId = 123L;
        String expectedUrl = BASE_URL + "/board/post/123";

        // When
        String result = notificationUrlAdapter.generatePostUrl(postId);

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
        String result = notificationUrlAdapter.generateRollingPaperUrl(userName);

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
        String result = notificationUrlAdapter.generateRollingPaperUrl(userName);

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
        String result = notificationUrlAdapter.generateRollingPaperUrl(userName);

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
        String result = notificationUrlAdapter.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("BaseUrl이 다른 경우 - 개발환경")
    void shouldGenerateUrls_WhenDifferentBaseUrl() {
        // Given
        String devBaseUrl = "http://localhost:3000";
        NotificationUrlAdapter devUrlGenerator = new NotificationUrlAdapter(devBaseUrl);
        Long postId = 456L;
        String userName = "devuser";

        // When
        String postUrl = devUrlGenerator.generatePostUrl(postId);
        String paperUrl = devUrlGenerator.generateRollingPaperUrl(userName);

        // Then
        assertThat(postUrl).isEqualTo("http://localhost:3000/board/post/456");
        assertThat(paperUrl).isEqualTo("http://localhost:3000/rolling-paper/devuser");
    }

    @Test
    @DisplayName("null 값 처리 - postId가 null인 경우")
    void shouldGeneratePostUrl_WhenPostIdIsNull() {
        // Given
        Long postId = null;
        String expectedUrl = BASE_URL + "/board/post/null";

        // When
        String result = notificationUrlAdapter.generatePostUrl(postId);

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
        String result = notificationUrlAdapter.generateRollingPaperUrl(userName);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

}