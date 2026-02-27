package jaeik.bimillog.unit.domain.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.SseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>SseMessage 도메인 값 객체 테스트</h2>
 * <p>SSE 메시지 값 객체의 생성과 기능을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("SseMessage 도메인 값 객체 테스트")
@Tag("unit")
class SseMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SSE 메시지 생성 - of 팩터리 메서드")
    void shouldCreateSseMessage_WhenValidInput() {
        // Given
        Long memberId = 1L;
        NotificationType type = NotificationType.COMMENT;
        String message = "댓글이 작성되었습니다!";
        String url = "/board/post/123";

        // When
        SseMessage sseMessage = SseMessage.of(memberId, type, message, url);

        // Then
        assertThat(sseMessage.memberId()).isEqualTo(memberId);
        assertThat(sseMessage.type()).isEqualTo(type);
        assertThat(sseMessage.message()).isEqualTo(message);
        assertThat(sseMessage.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("SSE 메시지 생성 - 생성자 직접 호출")
    void shouldCreateSseMessage_WhenUsingConstructor() {
        // Given
        Long memberId = 2L;
        NotificationType type = NotificationType.MESSAGE;
        String message = "롤링페이퍼에 메시지가 작성되었어요!";
        String url = "/rolling-paper/testuser";

        // When
        SseMessage sseMessage = new SseMessage(memberId, type, message, url);

        // Then
        assertThat(sseMessage.memberId()).isEqualTo(memberId);
        assertThat(sseMessage.type()).isEqualTo(type);
        assertThat(sseMessage.message()).isEqualTo(message);
        assertThat(sseMessage.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("JSON 데이터 변환 - toJsonData 메서드")
    void shouldConvertToJsonData_WhenCalled() throws Exception {
        // Given
        SseMessage sseMessage = SseMessage.of(
                1L,
                NotificationType.POST_FEATURED_WEEKLY,
                "축하합니다! 인기글에 선정되었습니다!",
                "/board/post/456"
        );

        // When
        String jsonData = sseMessage.toJsonData();
        JsonNode node = objectMapper.readTree(jsonData);

        // Then
        assertThat(node.get("message").asText()).isEqualTo("축하합니다! 인기글에 선정되었습니다!");
        assertThat(node.get("url").asText()).isEqualTo("/board/post/456");
    }

    @Test
    @DisplayName("동일성 검증 - equals와 hashCode")
    void shouldBeEqual_WhenSameValues() {
        // Given
        SseMessage sseMessage1 = SseMessage.of(
                1L,
                NotificationType.COMMENT,
                "댓글 알림",
                "/board/post/123"
        );
        
        SseMessage sseMessage2 = SseMessage.of(
                1L,
                NotificationType.COMMENT,
                "댓글 알림",
                "/board/post/123"
        );

        // When & Then
        assertThat(sseMessage1).isEqualTo(sseMessage2);
        assertThat(sseMessage1.hashCode()).isEqualTo(sseMessage2.hashCode());
    }

    @Test
    @DisplayName("문자열 표현 - toString 메서드")
    void shouldHaveStringRepresentation() {
        // Given
        SseMessage sseMessage = SseMessage.of(
                1L,
                NotificationType.INITIATE,
                "이벤트 스트림이 생성되었습니다.",
                ""
        );

        // When
        String stringRepresentation = sseMessage.toString();

        // Then
        assertThat(stringRepresentation).contains("SseMessage");
        assertThat(stringRepresentation).contains("memberId=1");
        assertThat(stringRepresentation).contains("type=INITIATE");
        assertThat(stringRepresentation).contains("message=이벤트 스트림이 생성되었습니다.");
    }

    @ParameterizedTest(name = "알림 타입: {0}")
    @EnumSource(NotificationType.class)
    @DisplayName("다양한 알림 타입 테스트")
    void shouldSupportDifferentNotificationTypes(NotificationType type) {
        // Given
        Long memberId = 1L;
        String message = "테스트 알림 메시지";
        String url = "/test/url";

        // When
        SseMessage sseMessage = SseMessage.of(memberId, type, message, url);

        // Then
        assertThat(sseMessage.type()).isEqualTo(type);
        assertThat(sseMessage.memberId()).isEqualTo(memberId);
        assertThat(sseMessage.message()).isEqualTo(message);
        assertThat(sseMessage.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("특수 문자가 포함된 메시지 처리")
    void shouldHandleSpecialCharactersInMessage() throws Exception {
        // Given
        String messageWithSpecialChars = "🎉 축하합니다! <게시글>이 \"인기글\"에 선정되었습니다! & 더 많은 혜택을...";
        String urlWithSpecialChars = "/board/post/123?ref=notification&type=featured";

        // When
        SseMessage sseMessage = SseMessage.of(
                1L,
                NotificationType.POST_FEATURED_WEEKLY,
                messageWithSpecialChars,
                urlWithSpecialChars
        );

        // Then
        assertThat(sseMessage.message()).isEqualTo(messageWithSpecialChars);
        assertThat(sseMessage.url()).isEqualTo(urlWithSpecialChars);
        
        JsonNode node = objectMapper.readTree(sseMessage.toJsonData());
        assertThat(node.get("message").asText()).isEqualTo(messageWithSpecialChars);
        assertThat(node.get("url").asText()).isEqualTo(urlWithSpecialChars);
    }

    @Test
    @DisplayName("빈 URL 처리")
    void shouldHandleEmptyUrl() throws Exception {
        // Given
        SseMessage sseMessage = SseMessage.of(
                1L,
                NotificationType.INITIATE,
                "초기화 메시지",
                ""
        );

        // When
        JsonNode node = objectMapper.readTree(sseMessage.toJsonData());

        // Then
        assertThat(sseMessage.url()).isEmpty();
        assertThat(node.get("url").asText()).isEqualTo("");
    }
}
